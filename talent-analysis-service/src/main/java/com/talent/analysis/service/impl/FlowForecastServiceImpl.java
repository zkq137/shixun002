package com.talent.analysis.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.analysis.mapper.AnalysisMapper;
import com.talent.analysis.service.FlowForecastService;
import com.talent.analysis.vo.FlowForecastVO;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 人才流动趋势预测。
 *
 * <p>流入使用历史月度数据训练季节调整线性趋势；流出/晋升缺少历史事件时，
 * 使用风险池和高潜池的业务基线估算，并在结果中明确降低置信度。
 */
@Service
@RequiredArgsConstructor
public class FlowForecastServiceImpl implements FlowForecastService {

    private static final int HISTORY_MONTHS = 24;
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter RUN_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final JdbcTemplate jdbcTemplate;
    private final AnalysisMapper analysisMapper;
    private final ObjectMapper objectMapper;

    @Value("${talent.dataset-version:dataset-v2}")
    private String datasetVersion;

    @Value("${talent.model-version:v1.2}")
    private String modelVersion;

    @PostConstruct
    public void initSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS flow_forecast_run (
                  run_code VARCHAR(40) NOT NULL,
                  horizon_months INT NOT NULL,
                  model_version VARCHAR(32) NOT NULL,
                  generated_at DATETIME NOT NULL,
                  metrics TEXT NOT NULL,
                  notes TEXT NOT NULL,
                  status VARCHAR(16) NOT NULL,
                  source VARCHAR(16) NOT NULL DEFAULT 'generated',
                  PRIMARY KEY (run_code),
                  KEY idx_forecast_time (generated_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS flow_forecast_point (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  run_code VARCHAR(40) NOT NULL,
                  movement_type VARCHAR(16) NOT NULL,
                  month CHAR(7) NOT NULL,
                  predicted_value DECIMAL(10,2) NOT NULL,
                  lower_bound DECIMAL(10,2) NOT NULL,
                  upper_bound DECIMAL(10,2) NOT NULL,
                  method VARCHAR(64) NOT NULL,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_forecast_point (run_code, movement_type, month),
                  KEY idx_forecast_run (run_code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    @Override
    public FlowForecastVO generate(int horizonMonths) {
        int horizon = Math.max(1, Math.min(horizonMonths, 12));
        YearMonth current = YearMonth.now();
        List<YearMonth> historyMonths = new ArrayList<>();
        YearMonth start = current.minusMonths(HISTORY_MONTHS);
        for (int i = 0; i < HISTORY_MONTHS; i++) {
            historyMonths.add(start.plusMonths(i));
        }

        List<Double> joinHistory = monthlyHistory("入职", historyMonths, current);
        List<Double> leaveHistory = monthlyHistory("离职", historyMonths, current);
        List<Double> promoteHistory = monthlyHistory("晋升", historyMonths, current);

        List<FlowForecastVO.Series> series = new ArrayList<>();
        FlowForecastVO.Series joinSeries = timeSeriesForecast(
                "inflow", "人才流入", joinHistory, historyMonths.size(), horizon, current);
        series.add(joinSeries);

        long nonZeroLeave = leaveHistory.stream().filter(value -> value > 0).count();
        long nonZeroPromote = promoteHistory.stream().filter(value -> value > 0).count();
        if (nonZeroLeave >= 6) {
            series.add(timeSeriesForecast(
                    "outflow", "人才流出", leaveHistory, historyMonths.size(), horizon, current));
        } else {
            series.add(poolBaselineForecast(
                    "outflow", "人才流出", estimatedMonthlyOutflow(), horizon, current,
                    "风险池业务基线", "低", "离职历史事件不足，使用当前风险池估算"));
        }
        if (nonZeroPromote >= 6) {
            series.add(timeSeriesForecast(
                    "promotion", "人才晋升", promoteHistory, historyMonths.size(), horizon, current));
        } else {
            series.add(poolBaselineForecast(
                    "promotion", "人才晋升", estimatedMonthlyPromotion(), horizon, current,
                    "高潜池业务基线", "低", "晋升历史事件不足，使用当前人才池估算"));
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("historyMonths", HISTORY_MONTHS);
        metrics.put("horizonMonths", horizon);
        metrics.put("datasetVersion", datasetVersion);
        metrics.put("sourceModelVersion", modelVersion);
        metrics.put("inflow", seriesMetrics(joinSeries));
        metrics.put("outflow", Map.of(
                "method", series.get(1).getMethod(), "confidence", series.get(1).getConfidence()));
        metrics.put("promotion", Map.of(
                "method", series.get(2).getMethod(), "confidence", series.get(2).getConfidence()));

        List<String> notes = List.of(
                "人才流入基于近 24 个完整月份的季节调整线性趋势预测",
                "离职历史事件不足时使用高风险、中风险和普通在职员工的风险池基线估算",
                "晋升历史事件不足时使用高潜人才和储备人才池的业务基线估算",
                "低置信度结果用于战略情景规划，不应作为个人人事决策依据"
        );

        String runCode = "FF-" + LocalDateTime.now().format(RUN_FORMAT);
        String version = "flow-forecast-v1.0";
        persistRun(runCode, horizon, version, metrics, notes, "generated", series);
        recordForecastLog(runCode, version, historyMonths.size(), horizon, metrics, notes);
        return loadRun(runCode);
    }

    @Override
    public FlowForecastVO latest() {
        List<String> rows = jdbcTemplate.queryForList(
                "select run_code from flow_forecast_run order by generated_at desc, run_code desc limit 1",
        String.class);
        if (rows.isEmpty()) {
            return null;
        }
        return loadRun(rows.get(0));
    }

    @Override
    public void exportCsv(HttpServletResponse response) throws IOException {
        FlowForecastVO forecast = latest();
        if (forecast == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                    "尚未生成人才流动预测");
        }
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(
                        "人才流动预测-" + forecast.getRunCode() + ".csv", StandardCharsets.UTF_8));
        PrintWriter writer = response.getWriter();
        writer.write('\ufeff');
        writer.println("movementType,month,predictedValue,lowerBound,upperBound,method");
        for (FlowForecastVO.Series series : forecast.getSeries()) {
            for (FlowForecastVO.Point point : series.getPoints()) {
                writer.printf(Locale.ROOT, "%s,%s,%.2f,%.2f,%.2f,%s%n",
                        series.getType(), point.getMonth(), point.getPredictedValue(),
                        point.getLowerBound(), point.getUpperBound(), series.getMethod());
            }
        }
    }

    @Override
    public FlowForecastVO importCsv(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "请选择 CSV 文件");
        }
        List<ImportedPoint> imported = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    if (line.startsWith("movementType")) continue;
                }
                if (line.isBlank()) continue;
                String[] cells = line.replace("\ufeff", "").split(",", -1);
                if (cells.length < 6) {
                    throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                            "CSV 格式错误，应有 6 列");
                }
                String type = cells[0].trim();
                String month = cells[1].trim();
                if (!List.of("inflow", "outflow", "promotion").contains(type)
                        || !month.matches("\\d{4}-\\d{2}")) {
                    throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                            "不支持的流动类型或月份格式：" + type + " " + month);
                }
                imported.add(new ImportedPoint(
                        type, month, Double.parseDouble(cells[2]), Double.parseDouble(cells[3]),
                        Double.parseDouble(cells[4]), cells[5].trim()));
                if (imported.size() > 500) {
                    throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                            "预测数据不能超过 500 行");
                }
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "预测值必须是数字");
        }
        if (imported.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "CSV 中没有有效预测数据");
        }

        Map<String, List<ImportedPoint>> grouped = new LinkedHashMap<>();
        for (ImportedPoint point : imported) {
            grouped.computeIfAbsent(point.type(), ignored -> new ArrayList<>()).add(point);
        }
        List<FlowForecastVO.Series> series = new ArrayList<>();
        for (String type : List.of("inflow", "outflow", "promotion")) {
            List<ImportedPoint> points = grouped.getOrDefault(type, List.of());
            if (points.isEmpty()) continue;
            FlowForecastVO.Series value = new FlowForecastVO.Series();
            value.setType(type);
            value.setName(typeName(type));
            value.setMethod(points.get(0).method());
            value.setConfidence("导入");
            value.setPoints(points.stream().map(point -> {
                FlowForecastVO.Point item = new FlowForecastVO.Point();
                item.setMonth(point.month());
                item.setPredictedValue(point.predicted());
                item.setLowerBound(point.lower());
                item.setUpperBound(point.upper());
                return item;
            }).toList());
            value.setTotalPredicted(round1(points.stream()
                    .mapToDouble(ImportedPoint::predicted).sum()));
            series.add(value);
        }
        if (series.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "缺少 inflow、outflow 或 promotion 预测数据");
        }

        int horizon = imported.stream()
                .map(ImportedPoint::month)
                .collect(java.util.stream.Collectors.toSet())
                .size();
        Map<String, Object> metrics = Map.of(
                "historyMonths", 0,
                "horizonMonths", horizon,
                "datasetVersion", datasetVersion,
                "source", "imported");
        List<String> notes = List.of("该预测批次由外部 CSV 导入，未执行本地模型计算");
        String runCode = "FF-IMP-" + LocalDateTime.now().format(RUN_FORMAT);
        persistRun(runCode, horizon, "imported-forecast", metrics, notes, "imported", series);
        recordForecastLog(runCode, "imported-forecast", imported.size(), horizon, metrics, notes);
        return loadRun(runCode);
    }

    private List<Double> monthlyHistory(String type, List<YearMonth> months, YearMonth current) {
        Map<YearMonth, Double> values = new HashMap<>();
        jdbcTemplate.query("""
                select date_format(effect_date, '%Y-%m') as month, count(*) as cnt
                from emp_movement
                where movement_type = ? and effect_date >= ? and effect_date < ?
                group by month
                """, rs -> {
            values.put(YearMonth.parse(rs.getString("month")), rs.getDouble("cnt"));
        }, type, months.get(0).atDay(1), current.atDay(1));
        return months.stream().map(month -> values.getOrDefault(month, 0.0)).toList();
    }

    private FlowForecastVO.Series timeSeriesForecast(
            String type, String name, List<Double> history, int historyMonths,
            int horizon, YearMonth current) {
        int n = history.size();
        double meanX = (n - 1) / 2.0;
        double meanY = history.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < n; i++) {
            numerator += (i - meanX) * (history.get(i) - meanY);
            denominator += (i - meanX) * (i - meanX);
        }
        double slope = denominator == 0 ? 0 : numerator / denominator;
        double intercept = meanY - slope * meanX;

        double[] seasonalSum = new double[12];
        int[] seasonalCount = new int[12];
        for (int i = 0; i < n; i++) {
            int monthIndex = current.minusMonths(historyMonths - i).getMonthValue() - 1;
            seasonalSum[monthIndex] += history.get(i) - (intercept + slope * i);
            seasonalCount[monthIndex]++;
        }
        double[] seasonal = new double[12];
        for (int i = 0; i < 12; i++) {
            seasonal[i] = seasonalCount[i] == 0 ? 0 : seasonalSum[i] / seasonalCount[i];
        }

        double residualSum = 0;
        double absoluteError = 0;
        double totalSum = history.stream().mapToDouble(Double::doubleValue).sum();
        double squaredTotal = history.stream()
                .mapToDouble(value -> (value - meanY) * (value - meanY)).sum();
        for (int i = 0; i < n; i++) {
            YearMonth month = current.minusMonths(historyMonths - i);
            double fitted = Math.max(0, intercept + slope * i
                    + seasonal[month.getMonthValue() - 1]);
            double residual = history.get(i) - fitted;
            residualSum += residual * residual;
            absoluteError += Math.abs(residual);
        }
        double residualStd = Math.sqrt(residualSum / Math.max(1, n - 2));
        double r2 = squaredTotal == 0 ? 0 : 1 - residualSum / squaredTotal;
        double mae = absoluteError / Math.max(1, n);

        FlowForecastVO.Series series = new FlowForecastVO.Series();
        series.setType(type);
        series.setName(name);
        series.setMethod("seasonal-linear-trend");
        series.setConfidence(r2 >= 0.5 && n >= 12 ? "中" : "低");
        double total = 0;
        for (int i = 1; i <= horizon; i++) {
            YearMonth month = current.plusMonths(i);
            double base = intercept + slope * (n - 1 + i);
            double predicted = Math.max(0, base + seasonal[month.getMonthValue() - 1]);
            double margin = 1.96 * Math.max(0.5, residualStd) * Math.sqrt(1 + 1.0 / n);
            FlowForecastVO.Point point = new FlowForecastVO.Point();
            point.setMonth(month.format(MONTH_FORMAT));
            point.setPredictedValue(round1(predicted));
            point.setLowerBound(round1(Math.max(0, predicted - margin)));
            point.setUpperBound(round1(predicted + margin));
            series.getPoints().add(point);
            total += point.getPredictedValue();
        }
        series.setTotalPredicted(round1(total));
        return series;
    }

    private FlowForecastVO.Series poolBaselineForecast(
            String type, String name, double monthlyBase, int horizon, YearMonth current,
            String method, String confidence, String note) {
        FlowForecastVO.Series series = new FlowForecastVO.Series();
        series.setType(type);
        series.setName(name);
        series.setMethod(method);
        series.setConfidence(confidence);
        double total = 0;
        for (int i = 1; i <= horizon; i++) {
            FlowForecastVO.Point point = new FlowForecastVO.Point();
            point.setMonth(current.plusMonths(i).format(MONTH_FORMAT));
            point.setPredictedValue(round1(monthlyBase));
            point.setLowerBound(round1(monthlyBase * 0.7));
            point.setUpperBound(round1(monthlyBase * 1.3));
            series.getPoints().add(point);
            total += point.getPredictedValue();
        }
        series.setTotalPredicted(round1(total));
        return series;
    }

    private double estimatedMonthlyOutflow() {
        long total = nvl(analysisMapper.countEmployees());
        long high = nvl(analysisMapper.countHighRisk());
        long middle = nvl(analysisMapper.countMiddleRisk());
        long normal = Math.max(0, total - high - middle);
        return high * 0.05 + middle * 0.02 + normal * 0.001;
    }

    private double estimatedMonthlyPromotion() {
        long highPotential = nvl(analysisMapper.countHighPotential());
        long reserve = nvl(analysisMapper.countReserveTalent());
        return highPotential * 0.012 + reserve * 0.004;
    }

    private void persistRun(String runCode, int horizon, String version,
                            Map<String, Object> metrics, List<String> notes,
                            String source, List<FlowForecastVO.Series> series) {
        jdbcTemplate.update("""
                insert into flow_forecast_run (
                  run_code, horizon_months, model_version, generated_at, metrics, notes, status, source
                ) values (?, ?, ?, now(), ?, ?, '已完成', ?)
                """, runCode, horizon, version, json(metrics), json(notes), source);
        for (FlowForecastVO.Series item : series) {
            for (FlowForecastVO.Point point : item.getPoints()) {
                jdbcTemplate.update("""
                        insert into flow_forecast_point (
                          run_code, movement_type, month, predicted_value,
                          lower_bound, upper_bound, method
                        ) values (?, ?, ?, ?, ?, ?, ?)
                        """, runCode, item.getType(), point.getMonth(), point.getPredictedValue(),
                        point.getLowerBound(), point.getUpperBound(), item.getMethod());
            }
        }
    }

    private void recordForecastLog(String runCode, String version, int sampleCount,
                                   int horizon, Map<String, Object> metrics, List<String> notes) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("algorithm", "seasonal-linear-trend + pool-baseline");
        params.put("historyMonths", HISTORY_MONTHS);
        params.put("horizonMonths", horizon);
        params.put("runCode", runCode);
        params.put("source", "flow-forecast");
        jdbcTemplate.update("""
                insert into model_log (
                  model_name, model_type, version, train_time, status, sample_count,
                  train_duration_ms, metrics, params, dataset_version, remark
                ) values ('人才流动趋势预测', '时间序列模型', ?, now(), '已完成', ?, 0, ?, ?, ?, ?)
                """, version, sampleCount, json(metrics), json(params), datasetVersion,
                "预测批次 " + runCode + "；" + String.join("；", notes));
    }

    private FlowForecastVO loadRun(String runCode) {
        Map<String, Object> run = jdbcTemplate.queryForMap("""
                select run_code, horizon_months, model_version,
                       date_format(generated_at, '%Y-%m-%d %H:%i') as generated_at,
                       metrics, notes, status, source
                from flow_forecast_run where run_code = ?
                """, runCode);
        FlowForecastVO vo = new FlowForecastVO();
        vo.setRunCode(runCode);
        vo.setHorizonMonths(((Number) run.get("horizon_months")).intValue());
        vo.setModelVersion(String.valueOf(run.get("model_version")));
        vo.setGeneratedAt(String.valueOf(run.get("generated_at")));
        vo.setStatus(String.valueOf(run.get("status")));
        vo.setSource(String.valueOf(run.get("source")));
        vo.setMetrics(readMap(String.valueOf(run.get("metrics"))));
        vo.setNotes(readList(String.valueOf(run.get("notes"))));

        Map<String, FlowForecastVO.Series> grouped = new LinkedHashMap<>();
        jdbcTemplate.query("""
                select movement_type, month, predicted_value, lower_bound, upper_bound, method
                from flow_forecast_point where run_code = ?
                order by field(movement_type, 'inflow', 'outflow', 'promotion'), month
                """, rs -> {
            String type = rs.getString("movement_type");
            FlowForecastVO.Series series = grouped.computeIfAbsent(type, key -> {
                FlowForecastVO.Series value = new FlowForecastVO.Series();
                value.setType(key);
                value.setName(typeName(key));
                value.setMethod(rsString(rs, "method"));
                value.setConfidence("promotion".equals(key) || "outflow".equals(key)
                        ? "低" : "中");
                return value;
            });
            FlowForecastVO.Point point = new FlowForecastVO.Point();
            point.setMonth(rs.getString("month"));
            point.setPredictedValue(rs.getDouble("predicted_value"));
            point.setLowerBound(rs.getDouble("lower_bound"));
            point.setUpperBound(rs.getDouble("upper_bound"));
            series.getPoints().add(point);
        }, runCode);
        grouped.values().forEach(series -> series.setTotalPredicted(round1(
                series.getPoints().stream().mapToDouble(FlowForecastVO.Point::getPredictedValue).sum())));
        vo.setSeries(new ArrayList<>(grouped.values()));
        return vo;
    }

    private Map<String, Object> seriesMetrics(FlowForecastVO.Series series) {
        return Map.of(
                "method", series.getMethod(),
                "confidence", series.getConfidence(),
                "totalPredicted", series.getTotalPredicted());
    }

    private String typeName(String type) {
        return switch (type) {
            case "inflow" -> "人才流入";
            case "outflow" -> "人才流出";
            case "promotion" -> "人才晋升";
            default -> type;
        };
    }

    private String rsString(java.sql.ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (Exception e) {
            return "";
        }
    }

    private long nvl(Long value) {
        return value == null ? 0 : value;
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "JSON 序列化失败");
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<String> readList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    private record ImportedPoint(String type, String month, double predicted,
                                 double lower, double upper, String method) { }
}
