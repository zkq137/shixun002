package com.talent.analysis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.analysis.dto.ReportRequest;
import com.talent.analysis.mapper.ReportMapper;
import com.talent.analysis.service.ReportService;
import com.talent.analysis.vo.ReportOptionVO;
import com.talent.analysis.vo.ReportResultVO;
import com.talent.analysis.vo.ReportVO;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义报表实现。
 *
 * <p><b>为什么用 JdbcTemplate 而不是 MyBatis：</b>维度、指标是用户临时组合的，SQL 结构不固定，
 * MyBatis 的注解 SQL 写不出来（用 ${} 拼接又会带来注入风险）。这里所有维度/指标/过滤字段
 * 都来自下面三张白名单，用户传的只是 key，值一律走 {@code ?} 参数占位，所以拼出来的 SQL 是安全的。
 *
 * <p>每张报表都会记下生成时用的<b>数据版本</b>和<b>模型版本</b>，配合模型日志可以回答
 * 「这个数字是哪份数据、哪个模型算出来的」——这就是报表溯源。
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    /** 单张报表最多返回多少行 */
    private static final int MAX_ROWS = 500;

    /** 维度白名单：key -> {显示名, SQL 表达式} */
    private static final Map<String, String[]> DIMENSIONS = new LinkedHashMap<>();

    /** 指标白名单：key -> {显示名, SQL 聚合表达式} */
    private static final Map<String, String[]> METRICS = new LinkedHashMap<>();

    /** 过滤字段白名单：key -> 列名 */
    private static final Map<String, String> FILTERS = new LinkedHashMap<>();

    static {
        DIMENSIONS.put("department", new String[]{"部门", "e.department"});
        DIMENSIONS.put("levelTier", new String[]{"职级层级", "e.level_tier"});
        DIMENSIONS.put("position", new String[]{"岗位", "p.position_name"});
        DIMENSIONS.put("positionType", new String[]{"岗位类型", "p.position_type"});
        DIMENSIONS.put("potentialLevel", new String[]{"潜力等级", "po.potential_level"});
        DIMENSIONS.put("talentTag", new String[]{"人才标签", "t.tag_name"});
        DIMENSIONS.put("warningLevel", new String[]{"流失风险", "w.warning_level"});
        DIMENSIONS.put("workMode", new String[]{"办公方式", "e.work_mode"});
        DIMENSIONS.put("gender", new String[]{"性别", "e.gender"});
        DIMENSIONS.put("ageRange", new String[]{"年龄段",
                "case when e.age < 25 then '25岁以下' when e.age < 30 then '25-29岁' "
                        + "when e.age < 35 then '30-34岁' when e.age < 40 then '35-39岁' "
                        + "when e.age < 50 then '40-49岁' else '50岁以上' end"});
        DIMENSIONS.put("tenureRange", new String[]{"司龄段",
                "case when e.tenure_years < 1 then '1年以内' when e.tenure_years < 3 then '1-3年' "
                        + "when e.tenure_years < 5 then '3-5年' when e.tenure_years < 10 then '5-10年' "
                        + "else '10年以上' end"});
        DIMENSIONS.put("perfLevel", new String[]{"绩效档位",
                "case when ifnull(f.score, 0) >= 4.5 then '5分' when f.score >= 3.5 then '4分' "
                        + "when f.score >= 2.5 then '3分' else '2分及以下' end"});

        METRICS.put("count", new String[]{"人数", "count(distinct e.id)"});
        METRICS.put("avgAge", new String[]{"平均年龄", "round(avg(e.age), 1)"});
        METRICS.put("avgTenure", new String[]{"平均司龄(年)", "round(avg(e.tenure_years), 1)"});
        METRICS.put("avgSalary", new String[]{"平均年薪(元)", "round(avg(s.base_salary), 0)"});
        METRICS.put("avgPerf", new String[]{"平均绩效", "round(avg(f.score), 2)"});
        METRICS.put("avgPotential", new String[]{"平均潜力分", "round(avg(po.total_score), 1)"});
        METRICS.put("avgRiskScore", new String[]{"平均流失风险分", "round(avg(w.risk_score), 1)"});
        METRICS.put("highRiskCount", new String[]{"高风险人数",
                "sum(case when w.warning_level = '高风险' then 1 else 0 end)"});
        METRICS.put("middleRiskCount", new String[]{"中风险人数",
                "sum(case when w.warning_level = '中风险' then 1 else 0 end)"});
        METRICS.put("coreTalentCount", new String[]{"核心骨干人数",
                "sum(case when t.tag_name = '核心骨干' then 1 else 0 end)"});
        METRICS.put("reserveTalentCount", new String[]{"储备人才人数",
                "sum(case when t.tag_name = '储备人才' then 1 else 0 end)"});
        METRICS.put("highPotentialCount", new String[]{"高潜人数",
                "sum(case when po.potential_level in ('S级（高潜）', 'A级（优秀）') then 1 else 0 end)"});

        FILTERS.put("department", "e.department");
        FILTERS.put("levelTier", "e.level_tier");
        FILTERS.put("positionType", "p.position_type");
        FILTERS.put("potentialLevel", "po.potential_level");
        FILTERS.put("talentTag", "t.tag_name");
        FILTERS.put("warningLevel", "w.warning_level");
        FILTERS.put("workMode", "e.work_mode");
        FILTERS.put("gender", "e.gender");
    }

    private final ReportMapper reportMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${talent.perf-year:2026}")
    private int perfYear;

    @Value("${talent.dataset-version:dataset-v2}")
    private String dataVersion;

    @Value("${talent.model-version:v1.2}")
    private String modelVersion;

    @Override
    public List<ReportOptionVO> options() {
        List<ReportOptionVO> options = new ArrayList<>();
        DIMENSIONS.forEach((key, value) -> options.add(new ReportOptionVO(key, value[0], "dimension")));
        METRICS.forEach((key, value) -> options.add(new ReportOptionVO(key, value[0], "metric")));
        return options;
    }

    @Override
    public ReportResultVO generate(ReportRequest request) {
        List<String> dimensions = sanitize(request.getDimensions(), DIMENSIONS, "维度");
        List<String> metrics = sanitize(request.getMetrics(), METRICS, "指标");
        if (metrics.isEmpty()) {
            metrics = List.of("count");
        }

        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select ");
        for (int i = 0; i < dimensions.size(); i++) {
            sql.append(DIMENSIONS.get(dimensions.get(i))[1]).append(" as d").append(i).append(", ");
        }
        for (int j = 0; j < metrics.size(); j++) {
            sql.append(METRICS.get(metrics.get(j))[1]).append(" as m").append(j);
            sql.append(j == metrics.size() - 1 ? " " : ", ");
        }
        sql.append("from emp_employee e ")
                .append("left join position p on p.id = e.position_id ")
                .append("left join emp_salary s on s.employee_id = e.id ")
                .append("left join emp_performance f on f.employee_id = e.id and f.perf_year = ? ")
                .append("left join emp_potential po on po.employee_id = e.id ")
                .append("left join resign_warning_record w on w.employee_id = e.id ")
                .append("left join emp_talent_tag t on t.employee_id = e.id ")
                .append("where e.status = '在职' ");
        args.add(perfYear);

        if (request.getFilters() != null) {
            request.getFilters().forEach((key, value) -> {
                String column = FILTERS.get(key);
                if (column != null && StringUtils.hasText(value)) {
                    sql.append("and ").append(column).append(" = ? ");
                    args.add(value.trim());
                }
            });
        }
        if (!dimensions.isEmpty()) {
            sql.append("group by ");
            for (int i = 0; i < dimensions.size(); i++) {
                sql.append(DIMENSIONS.get(dimensions.get(i))[1]);
                sql.append(i == dimensions.size() - 1 ? " " : ", ");
            }
        }
        sql.append("order by m0 desc limit ").append(MAX_ROWS);

        List<Map<String, Object>> raw = jdbcTemplate.queryForList(sql.toString(), args.toArray());

        ReportResultVO result = new ReportResultVO();
        result.setReportName(StringUtils.hasText(request.getReportName())
                ? request.getReportName() : "自定义报表");
        result.setDataVersion(dataVersion);
        result.setModelVersion(modelVersion);
        result.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        for (String key : dimensions) {
            result.addColumn(key, DIMENSIONS.get(key)[0], "dimension");
        }
        for (String key : metrics) {
            result.addColumn(key, METRICS.get(key)[0], "number");
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> item : raw) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < dimensions.size(); i++) {
                row.put(dimensions.get(i), item.get("d" + i));
            }
            for (int j = 0; j < metrics.size(); j++) {
                row.put(metrics.get(j), normalize(item.get("m" + j)));
            }
            rows.add(row);
        }
        result.setRows(rows);
        result.setChart(buildChart(dimensions, metrics, rows));

        if (Boolean.TRUE.equals(request.getSave())) {
            result.setReportId(save(result, request, dimensions, metrics));
        }
        return result;
    }

    @Override
    public ReportResultVO generateFromTemplate(Long templateId) {
        ReportVO template = reportMapper.selectTemplateById(templateId);
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "报表模板不存在：" + templateId);
        }
        ReportRequest request;
        try {
            request = objectMapper.readValue(template.getContent(), ReportRequest.class);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "模板内容不合法：" + e.getMessage());
        }
        request.setReportName(template.getReportName());
        request.setSave(Boolean.TRUE);
        return generate(request);
    }

    @Override
    public List<ReportVO> history() {
        return reportMapper.selectReports();
    }

    @Override
    public ReportVO detail(Long id) {
        ReportVO report = id == null ? null : reportMapper.selectReportById(id);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "报表不存在：" + id);
        }
        return report;
    }

    @Override
    public void delete(Long id) {
        detail(id);
        reportMapper.deleteReport(id);
    }

    @Override
    public List<ReportVO> templates() {
        return reportMapper.selectTemplates();
    }

    @Override
    public Long saveTemplate(ReportRequest request) {
        if (!StringUtils.hasText(request.getReportName())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "模板名称不能为空");
        }
        List<String> dimensions = sanitize(request.getDimensions(), DIMENSIONS, "维度");
        List<String> metrics = sanitize(request.getMetrics(), METRICS, "指标");

        ReportVO template = new ReportVO();
        template.setReportName(request.getReportName().trim());
        template.setDimensions(String.join(",", dimensions));
        try {
            template.setContent(objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "模板保存失败：" + e.getMessage());
        }
        reportMapper.insertTemplate(template);
        return template.getId();
    }

    @Override
    public void deleteTemplate(Long id) {
        if (reportMapper.selectTemplateById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "报表模板不存在：" + id);
        }
        reportMapper.deleteTemplate(id);
    }

    /* ---------------- 内部方法 ---------------- */

    private Long save(ReportResultVO result, ReportRequest request, List<String> dimensions, List<String> metrics) {
        ReportVO report = new ReportVO();
        report.setReportName(result.getReportName());
        report.setDimensions(String.join(",", dimensions));
        report.setMetrics(String.join(",", metrics));
        report.setRowCount(result.getRows().size());
        report.setModelVersion(modelVersion);
        report.setDataVersion(dataVersion);
        report.setGeneratedBy("当前用户");
        try {
            report.setFilters(objectMapper.writeValueAsString(request.getFilters()));
            report.setReportData(objectMapper.writeValueAsString(result.getRows()));
            report.setChartData(objectMapper.writeValueAsString(result.getChart()));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "报表保存失败：" + e.getMessage());
        }
        reportMapper.insertReport(report);
        return report.getId();
    }

    /** 只保留白名单里的 key，避免有人塞奇怪的字段进来 */
    private List<String> sanitize(List<String> keys, Map<String, ?> whitelist, String what) {
        List<String> result = new ArrayList<>();
        if (keys == null) {
            return result;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if (!whitelist.containsKey(key)) {
                throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "不支持的" + what + "：" + key);
            }
            if (!result.contains(key)) {
                result.add(key);
            }
        }
        return result;
    }

    private ReportResultVO.ChartVO buildChart(List<String> dimensions, List<String> metrics,
                                              List<Map<String, Object>> rows) {
        ReportResultVO.ChartVO chart = new ReportResultVO.ChartVO();
        if (dimensions.isEmpty() || metrics.isEmpty() || rows.isEmpty()) {
            return chart;
        }
        String firstDim = dimensions.get(0);
        List<String> categories = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String value = String.valueOf(row.get(firstDim));
            if (!categories.contains(value)) {
                categories.add(value);
            }
        }
        chart.setCategories(categories);

        if (dimensions.size() == 1) {
            // 单维度：每个指标一条系列
            for (String metric : metrics) {
                ReportResultVO.SeriesVO series = new ReportResultVO.SeriesVO(METRICS.get(metric)[0]);
                for (String category : categories) {
                    Object value = rows.stream()
                            .filter(row -> category.equals(String.valueOf(row.get(firstDim))))
                            .map(row -> row.get(metric))
                            .findFirst().orElse(null);
                    series.getData().add(value);
                }
                chart.getSeries().add(series);
            }
        } else {
            // 多维度：第一个维度做横轴，第二个维度的取值做系列，只画第一个指标（多行相加）
            String secondDim = dimensions.get(1);
            String metric = metrics.get(0);
            List<String> seriesNames = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                String value = String.valueOf(row.get(secondDim));
                if (!seriesNames.contains(value)) {
                    seriesNames.add(value);
                }
            }
            for (String name : seriesNames) {
                ReportResultVO.SeriesVO series = new ReportResultVO.SeriesVO(name);
                for (String category : categories) {
                    double sum = rows.stream()
                            .filter(row -> category.equals(String.valueOf(row.get(firstDim)))
                                    && name.equals(String.valueOf(row.get(secondDim))))
                            .mapToDouble(row -> toDouble(row.get(metric)))
                            .sum();
                    series.getData().add(Math.round(sum * 10) / 10.0);
                }
                chart.getSeries().add(series);
            }
        }
        return chart;
    }

    private Object normalize(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.scale() <= 0 ? (Object) decimal.longValue() : (Object) decimal.doubleValue();
        }
        return value;
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0;
    }
}
