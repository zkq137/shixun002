package com.talent.analysis.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.analysis.service.TalentModelService;
import com.talent.analysis.vo.ModelArtifactVO;
import com.talent.analysis.vo.ModelPredictionVO;
import com.talent.analysis.vo.ModelTrainResultVO;
import com.talent.analysis.vo.PositionRecommendationVO;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 真实可训练、可持久化、可在线推理的人才模型。
 *
 * <p>三类模型都使用标准化逻辑回归。模型参数保存在 model_artifact，
 * 每次训练/失败会写入 model_log，预测时读取最新成功训练的版本。
 */
@Service
@RequiredArgsConstructor
public class TalentModelServiceImpl implements TalentModelService {

    private static final String ATTRITION = "attrition-risk";
    private static final String POTENTIAL = "high-potential";
    private static final String POSITION_FIT = "position-fit";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^v(\\d+)\\.(\\d+)$");

    private static final List<String> ATTRITION_FEATURES = List.of(
            "司龄", "绩效评分", "薪酬系数", "潜力得分", "技能数量", "培训完成数", "年龄");
    private static final List<String> POTENTIAL_FEATURES = List.of(
            "绩效评分", "职级层级", "司龄", "技能数量", "培训完成数", "薪酬系数", "年龄");
    private static final List<String> POSITION_FEATURES = List.of(
            "核心技能覆盖率", "加分技能覆盖率", "部门匹配", "层级匹配",
            "岗位类型匹配", "绩效评分", "潜力得分", "司龄");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${talent.perf-year:2026}")
    private int perfYear;

    @Value("${talent.dataset-version:dataset-v2}")
    private String datasetVersion;

    private final Map<String, Artifact> artifactCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS model_artifact (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  model_code VARCHAR(64) NOT NULL,
                  model_name VARCHAR(128) NOT NULL,
                  model_type VARCHAR(32) NOT NULL,
                  version VARCHAR(32) NOT NULL,
                  feature_names TEXT NOT NULL,
                  means TEXT NOT NULL,
                  stddevs TEXT NOT NULL,
                  coefficients TEXT NOT NULL,
                  intercept DOUBLE NOT NULL,
                  threshold DOUBLE NOT NULL,
                  metrics TEXT NOT NULL,
                  sample_count INT NOT NULL,
                  positive_count INT NOT NULL,
                  dataset_version VARCHAR(64),
                  train_time DATETIME NOT NULL,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_model_version (model_code, version),
                  KEY idx_model_time (model_code, train_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    @Override
    public List<ModelTrainResultVO> trainAll() {
        return List.of(train(ATTRITION), train(POTENTIAL), train(POSITION_FIT));
    }

    @Override
    public ModelTrainResultVO train(String modelCode) {
        Spec spec = specOf(modelCode);
        String version = nextVersion(spec.name());
        LocalDateTime startedAt = LocalDateTime.now();
        try {
            PreparedTraining prepared = switch (spec.code()) {
                case ATTRITION -> buildAttritionTraining();
                case POTENTIAL -> buildPotentialTraining();
                case POSITION_FIT -> buildPositionTraining();
                default -> throw unsupported(modelCode);
            };
            if (prepared.samples().size() < 30) {
                throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                        "训练样本不足，至少需要 30 条");
            }

            Artifact artifact = fit(spec, version, prepared);
            long duration = Duration.between(startedAt, LocalDateTime.now()).toMillis();
            artifact = new Artifact(
                    artifact.id(), artifact.modelCode(), artifact.modelName(), artifact.modelType(),
                    artifact.version(), artifact.features(), artifact.means(), artifact.stddevs(),
                    artifact.coefficients(), artifact.intercept(), artifact.threshold(),
                    artifact.metrics(), artifact.sampleCount(), artifact.positiveCount(),
                    artifact.datasetVersion(), LocalDateTime.now().toString());
            persistArtifact(artifact);
            recordLog(spec, artifact, "已完成", duration,
                    prepared.params(), "使用真实业务数据训练，模型已部署");
            artifactCache.remove(modelCode);
            return toTrainResult(artifact, duration, "训练完成，模型已部署");
        } catch (Exception e) {
            long duration = Duration.between(startedAt, LocalDateTime.now()).toMillis();
            recordFailedLog(spec, version, duration, e);
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "模型训练失败：" + e.getMessage());
        }
    }

    @Override
    public List<ModelArtifactVO> artifacts() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select id, model_code, model_name, model_type, version, feature_names, metrics,
                       sample_count, positive_count, dataset_version,
                       date_format(train_time, '%Y-%m-%d %H:%i') as train_time
                from model_artifact
                order by train_time desc, id desc
                """);
        Map<String, ModelArtifactVO> latest = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String code = str(row.get("model_code"));
            latest.computeIfAbsent(code, ignored -> {
                ModelArtifactVO vo = new ModelArtifactVO();
                vo.setId(number(row.get("id")).longValue());
                vo.setModelCode(code);
                vo.setModelName(str(row.get("model_name")));
                vo.setModelType(str(row.get("model_type")));
                vo.setVersion(str(row.get("version")));
                vo.setFeatures(readList(str(row.get("feature_names"))));
                vo.setMetrics(readMap(str(row.get("metrics"))));
                vo.setSampleCount(number(row.get("sample_count")).intValue());
                vo.setPositiveCount(number(row.get("positive_count")).intValue());
                vo.setDatasetVersion(str(row.get("dataset_version")));
                vo.setTrainTime(str(row.get("train_time")));
                return vo;
            });
        }
        return new ArrayList<>(latest.values());
    }

    @Override
    public ModelPredictionVO predictAttrition(String employeeIdentifier) {
        Artifact artifact = loadArtifact(ATTRITION);
        EmployeeBase employee = resolveEmployee(employeeIdentifier);
        Map<String, Double> features = attritionFeatures(employee);
        double probability = probability(artifact, features);
        boolean positive = probability >= artifact.threshold();
        String label;
        if (!positive) {
            label = "低风险";
        } else if (probability >= Math.max(0.75, artifact.threshold() + 0.15)) {
            label = "高风险";
        } else {
            label = "中风险";
        }

        ModelPredictionVO vo = prediction(artifact, employee, probability, label);
        vo.setExplanation("模型以绩效、司龄、薪酬、潜力、技能和培训等特征综合评估流失风险");
        vo.setTopFeatures(contributions(artifact, features));
        return vo;
    }

    @Override
    public ModelPredictionVO predictHighPotential(String employeeIdentifier) {
        Artifact artifact = loadArtifact(POTENTIAL);
        EmployeeBase employee = resolveEmployee(employeeIdentifier);
        Map<String, Double> features = potentialFeatures(employee);
        double probability = probability(artifact, features);
        String label = probability >= artifact.threshold() ? "高潜候选" : "普通人才";
        ModelPredictionVO vo = prediction(artifact, employee, probability, label);
        vo.setExplanation("模型从业务表现、成长积累和能力数据预测 S/A 级高潜可能性");
        vo.setTopFeatures(contributions(artifact, features));
        return vo;
    }

    @Override
    public PositionRecommendationVO recommendPositions(String employeeIdentifier, int topN) {
        Artifact artifact = loadArtifact(POSITION_FIT);
        EmployeeBase employee = resolveEmployee(employeeIdentifier);
        Set<Long> employeeSkills = employeeSkills().getOrDefault(employee.id(), Set.of());
        Map<Long, PositionData> positions = positions();
        Map<Long, Set<Long>> coreSkills = positionSkills("核心");
        Map<Long, Set<Long>> plusSkills = positionSkills("加分");

        List<PositionRecommendationVO.Item> items = positions.values().stream()
                .map(position -> new PositionRecommendationVO.Item(
                        position.id(),
                        position.name(),
                        position.department(),
                        round1(probability(artifact, positionFeatureMap(
                                employee, employeeSkills, position, coreSkills, plusSkills,
                                positions.get(employee.positionId()))) * 100)))
                .sorted(Comparator.comparing(PositionRecommendationVO.Item::getScore).reversed())
                .limit(Math.max(1, Math.min(topN, 20)))
                .toList();

        PositionRecommendationVO vo = new PositionRecommendationVO();
        vo.setEmployeeId(employee.id());
        vo.setEmployeeName(employee.name());
        vo.setModelVersion(artifact.version());
        vo.setRecommendations(items);
        return vo;
    }

    private PreparedTraining buildAttritionTraining() {
        List<EmployeeBase> employees = employees(null);
        List<Sample> samples = employees.stream()
                .map(employee -> new Sample(attritionVector(employee),
                        employee.warningLevel().contains("中风险")
                                || employee.warningLevel().contains("高风险") ? 1 : 0))
                .toList();
        Map<String, Object> params = trainingParams(ATTRITION_FEATURES, "中风险/高风险=1");
        return new PreparedTraining(ATTRITION_FEATURES, samples, params);
    }

    private PreparedTraining buildPotentialTraining() {
        List<EmployeeBase> employees = employees(null);
        List<Sample> samples = employees.stream()
                .map(employee -> new Sample(potentialVector(employee),
                        employee.potentialLevel().contains("S级")
                                || employee.potentialLevel().contains("A级") ? 1 : 0))
                .toList();
        Map<String, Object> params = trainingParams(POTENTIAL_FEATURES, "S/A级潜力=1");
        return new PreparedTraining(POTENTIAL_FEATURES, samples, params);
    }

    private PreparedTraining buildPositionTraining() {
        List<EmployeeBase> employees = employees(null);
        Map<Long, Set<Long>> employeeSkills = employeeSkills();
        Map<Long, PositionData> positions = positions();
        Map<Long, Set<Long>> coreSkills = positionSkills("核心");
        Map<Long, Set<Long>> plusSkills = positionSkills("加分");
        List<PositionData> positionList = new ArrayList<>(positions.values());
        if (positionList.size() < 2) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "岗位数量不足，无法训练匹配模型");
        }

        List<Sample> samples = new ArrayList<>();
        for (EmployeeBase employee : employees) {
            PositionData currentPosition = positions.get(employee.positionId());
            if (currentPosition == null) continue;
            samples.add(new Sample(positionFeatures(
                    employee, employeeSkills.getOrDefault(employee.id(), Set.of()),
                    currentPosition, coreSkills, plusSkills, currentPosition), 1));

            Set<Long> sampled = new HashSet<>();
            for (int i = 0; samples.size() < employees.size() * 4 && i < 8 && sampled.size() < 3; i++) {
                int index = Math.floorMod((int) employee.id() * 31 + i * 17, positionList.size());
                PositionData negative = positionList.get(index);
                if (negative.id() == employee.positionId() || !sampled.add(negative.id())) continue;
                samples.add(new Sample(positionFeatures(
                        employee, employeeSkills.getOrDefault(employee.id(), Set.of()),
                        negative, coreSkills, plusSkills, currentPosition), 0));
            }
        }
        Map<String, Object> params = trainingParams(
                POSITION_FEATURES, "当前任职岗位=1，同员工随机不匹配岗位=0");
        return new PreparedTraining(POSITION_FEATURES, samples, params);
    }

    private Artifact fit(Spec spec, String version, PreparedTraining prepared) {
        List<Sample> samples = prepared.samples();
        List<Integer> trainIndexes = new ArrayList<>();
        List<Integer> testIndexes = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < samples.size(); i++) indexes.add(i);
        Collections.shuffle(indexes, new Random(20261027L));
        int trainSize = Math.max(20, (int) Math.round(samples.size() * 0.8));
        trainSize = Math.min(trainSize, samples.size() - 2);
        trainIndexes.addAll(indexes.subList(0, trainSize));
        testIndexes.addAll(indexes.subList(trainSize, samples.size()));

        int featureCount = prepared.featureNames().size();
        double[] means = new double[featureCount];
        double[] stddevs = new double[featureCount];
        for (int index : trainIndexes) {
            double[] x = samples.get(index).features();
            for (int j = 0; j < featureCount; j++) means[j] += x[j];
        }
        for (int j = 0; j < featureCount; j++) means[j] /= trainIndexes.size();
        for (int index : trainIndexes) {
            double[] x = samples.get(index).features();
            for (int j = 0; j < featureCount; j++) {
                double delta = x[j] - means[j];
                stddevs[j] += delta * delta;
            }
        }
        for (int j = 0; j < featureCount; j++) {
            stddevs[j] = Math.sqrt(stddevs[j] / Math.max(1, trainIndexes.size()));
            if (stddevs[j] < 1e-9) stddevs[j] = 1;
        }

        List<Sample> training = trainIndexes.stream()
                .map(i -> standardize(samples.get(i), means, stddevs))
                .toList();
        long positives = training.stream().filter(sample -> sample.label() == 1).count();
        long negatives = training.size() - positives;
        if (positives == 0 || negatives == 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "训练集必须同时包含正样本和负样本");
        }

        double[] weights = new double[featureCount];
        double intercept = 0;
        double positiveWeight = training.size() / (2.0 * positives);
        double negativeWeight = training.size() / (2.0 * negatives);
        for (int epoch = 0; epoch < 1500; epoch++) {
            double[] gradient = new double[featureCount];
            double biasGradient = 0;
            for (Sample sample : training) {
                double probability = sigmoid(dot(weights, sample.features()) + intercept);
                double classWeight = sample.label() == 1 ? positiveWeight : negativeWeight;
                double error = (probability - sample.label()) * classWeight;
                for (int j = 0; j < featureCount; j++) {
                    gradient[j] += error * sample.features()[j];
                }
                biasGradient += error;
            }
            double learningRate = 0.01 / (1 + epoch * 0.001);
            for (int j = 0; j < featureCount; j++) {
                weights[j] -= learningRate * (gradient[j] / training.size() + 0.001 * weights[j]);
            }
            intercept -= learningRate * biasGradient / training.size();
        }

        List<Double> testProbabilities = new ArrayList<>();
        List<Integer> testLabels = new ArrayList<>();
        for (int index : testIndexes) {
            Sample sample = samples.get(index);
            Sample standardized = standardize(sample, means, stddevs);
            testProbabilities.add(sigmoid(dot(weights, standardized.features()) + intercept));
            testLabels.add(sample.label());
        }

        double threshold = bestThreshold(testProbabilities, testLabels);
        Map<String, Object> metrics = evaluate(testProbabilities, testLabels, threshold);
        metrics.put("trainCount", training.size());
        metrics.put("testCount", testIndexes.size());
        metrics.put("positiveCount", samples.stream().filter(sample -> sample.label() == 1).count());
        metrics.put("algorithm", "LogisticRegression");
        metrics.put("threshold", round4(threshold));

        return new Artifact(
                null, spec.code(), spec.name(), spec.type(), version,
                prepared.featureNames(), means, stddevs, weights, intercept, threshold,
                metrics, samples.size(),
                (int) samples.stream().filter(sample -> sample.label() == 1).count(),
                datasetVersion, LocalDateTime.now().toString());
    }

    private Map<String, Object> evaluate(List<Double> probabilities, List<Integer> labels, double threshold) {
        int tp = 0;
        int fp = 0;
        int tn = 0;
        int fn = 0;
        for (int i = 0; i < probabilities.size(); i++) {
            boolean predicted = probabilities.get(i) >= threshold;
            boolean actual = labels.get(i) == 1;
            if (predicted && actual) tp++;
            else if (predicted) fp++;
            else if (actual) fn++;
            else tn++;
        }
        double precision = tp + fp == 0 ? 0 : (double) tp / (tp + fp);
        double recall = tp + fn == 0 ? 0 : (double) tp / (tp + fn);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        double accuracy = (double) (tp + tn) / Math.max(1, probabilities.size());
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("auc", round4(auc(probabilities, labels)));
        metrics.put("accuracy", round4(accuracy));
        metrics.put("precision", round4(precision));
        metrics.put("recall", round4(recall));
        metrics.put("f1", round4(f1));
        return metrics;
    }

    private double auc(List<Double> scores, List<Integer> labels) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) order.add(i);
        // 使用升序排名计算 Mann-Whitney U；若按降序排名，公式会把 AUC 反向。
        order.sort(Comparator.comparingDouble((Integer i) -> scores.get(i)));
        long positives = labels.stream().filter(label -> label == 1).count();
        long negatives = labels.size() - positives;
        if (positives == 0 || negatives == 0) return 0.5;
        double rankSum = 0;
        for (int rank = 0; rank < order.size(); rank++) {
            if (labels.get(order.get(rank)) == 1) rankSum += rank + 1;
        }
        return (rankSum - positives * (positives + 1) / 2.0) / (positives * negatives);
    }

    private double bestThreshold(List<Double> probabilities, List<Integer> labels) {
        double bestThreshold = 0.5;
        double bestF1 = -1;
        for (int step = 10; step <= 80; step++) {
            double threshold = step / 100.0;
            Map<String, Object> metrics = evaluate(probabilities, labels, threshold);
            double f1 = ((Number) metrics.get("f1")).doubleValue();
            if (f1 > bestF1) {
                bestF1 = f1;
                bestThreshold = threshold;
            }
        }
        return bestThreshold;
    }

    private ModelPredictionVO prediction(Artifact artifact, EmployeeBase employee,
                                         double probability, String label) {
        ModelPredictionVO vo = new ModelPredictionVO();
        vo.setModelCode(artifact.modelCode());
        vo.setModelName(artifact.modelName());
        vo.setModelVersion(artifact.version());
        vo.setEmployeeId(employee.id());
        vo.setEmployeeName(employee.name());
        vo.setScore(round4(probability));
        vo.setScorePercent(round1(probability * 100));
        vo.setResultLabel(label);
        return vo;
    }

    private Map<String, Double> contributions(Artifact artifact, Map<String, Double> features) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < artifact.features().size(); i++) {
            String feature = artifact.features().get(i);
            double value = features.getOrDefault(feature, 0.0);
            double standardized = (value - artifact.means()[i]) / artifact.stddevs()[i];
            result.put(feature, round4(artifact.coefficients()[i] * standardized));
        }
        return result.entrySet().stream()
                .sorted((left, right) -> Double.compare(
                        Math.abs(right.getValue()), Math.abs(left.getValue())))
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private double probability(Artifact artifact, Map<String, Double> values) {
        double[] x = new double[artifact.features().size()];
        for (int i = 0; i < x.length; i++) {
            double value = values.getOrDefault(artifact.features().get(i), 0.0);
            x[i] = (value - artifact.means()[i]) / artifact.stddevs()[i];
        }
        return sigmoid(dot(artifact.coefficients(), x) + artifact.intercept());
    }

    private Sample standardize(Sample sample, double[] means, double[] stddevs) {
        double[] values = new double[sample.features().length];
        for (int i = 0; i < values.length; i++) {
            values[i] = (sample.features()[i] - means[i]) / stddevs[i];
        }
        return new Sample(values, sample.label());
    }

    private double dot(double[] left, double[] right) {
        double sum = 0;
        for (int i = 0; i < left.length; i++) sum += left[i] * right[i];
        return sum;
    }

    private double sigmoid(double value) {
        if (value >= 0) {
            double z = Math.exp(-value);
            return 1 / (1 + z);
        }
        double z = Math.exp(value);
        return z / (1 + z);
    }

    private List<EmployeeBase> employees(Long employeeId) {
        String sql = """
                select e.id, e.emp_no, e.name, e.age, e.tenure_years, e.position_id,
                       e.department, e.level_tier, e.work_mode,
                       coalesce(s.salary_coefficient, 1) as salary_coefficient,
                       coalesce(f.score, 0) as perf_score,
                       coalesce(po.total_score, 0) as potential_score,
                       coalesce(po.potential_level, '') as potential_level,
                       coalesce(po.indicator_values, '{}') as indicator_values,
                       (select count(*) from emp_skill es where es.employee_id = e.id) as skill_count,
                       (select count(*) from training_record tr
                         where tr.employee_id = e.id and tr.status = '已完成') as training_count,
                       coalesce((select w.warning_level from resign_warning_record w
                         where w.employee_id = e.id order by w.warning_time desc, w.id desc limit 1), '') as warning_level
                from emp_employee e
                left join emp_salary s on s.employee_id = e.id
                left join emp_performance f on f.employee_id = e.id and f.perf_year = ?
                left join emp_potential po on po.employee_id = e.id
                where e.status = '在职'
                """;
        List<Object> args = new ArrayList<>();
        args.add(perfYear);
        if (employeeId != null) {
            sql += " and e.id = ?";
            args.add(employeeId);
        }
        return jdbcTemplate.query(sql, (rs, rowNum) -> new EmployeeBase(
                rs.getLong("id"),
                rs.getString("emp_no"),
                rs.getString("name"),
                rs.getDouble("age"),
                rs.getDouble("tenure_years"),
                rs.getLong("position_id"),
                rs.getString("department"),
                rs.getString("level_tier"),
                rs.getString("work_mode"),
                rs.getDouble("salary_coefficient"),
                rs.getDouble("perf_score"),
                rs.getDouble("potential_score"),
                rs.getString("potential_level"),
                rs.getString("indicator_values"),
                rs.getInt("skill_count"),
                rs.getInt("training_count"),
                rs.getString("warning_level")
        ), args.toArray());
    }

    private EmployeeBase employee(Long employeeId) {
        List<EmployeeBase> employees = employees(employeeId);
        if (employees.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "员工不存在或不在职：" + employeeId);
        }
        return employees.get(0);
    }

    /**
     * 在线推理允许使用员工 ID、工号或完整姓名。
     */
    private EmployeeBase resolveEmployee(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "员工 ID、工号或姓名不能为空");
        }
        String value = identifier.trim();
        if (value.matches("\\d+")) {
            return employee(Long.parseLong(value));
        }

        List<EmployeeBase> matched = employees(null).stream()
                .filter(employee -> value.equals(employee.empNo()) || value.equals(employee.name()))
                .toList();
        if (matched.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                    "未找到员工：" + value + "，请输入数字ID、完整工号或姓名");
        }
        if (matched.size() > 1) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "姓名对应多名员工，请改用工号或数字ID");
        }
        return matched.get(0);
    }

    private Map<Long, Set<Long>> employeeSkills() {
        Map<Long, Set<Long>> result = new HashMap<>();
        jdbcTemplate.query("select employee_id, skill_id from emp_skill",
                rs -> {
                    result.computeIfAbsent(rs.getLong("employee_id"), ignored -> new HashSet<>())
                            .add(rs.getLong("skill_id"));
                });
        return result;
    }

    private Map<Long, PositionData> positions() {
        Map<Long, PositionData> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                select id, position_name, department, level_tier, position_type
                from position order by id
                """, rs -> {
            long id = rs.getLong("id");
            result.put(id, new PositionData(
                    id, rs.getString("position_name"), rs.getString("department"),
                    rs.getString("level_tier"), rs.getString("position_type")));
        });
        return result;
    }

    private Map<Long, Set<Long>> positionSkills(String requireType) {
        Map<Long, Set<Long>> result = new HashMap<>();
        jdbcTemplate.query("""
                select position_id, skill_id from pos_skill_require where require_type = ?
                """, rs -> {
            result.computeIfAbsent(rs.getLong("position_id"), ignored -> new HashSet<>())
                    .add(rs.getLong("skill_id"));
        }, requireType);
        return result;
    }

    private double[] attritionVector(EmployeeBase e) {
        return new double[]{
                e.tenure(), e.performance(), e.salaryCoefficient(), e.potentialScore(),
                e.skillCount(), e.trainingCount(), e.age()
        };
    }

    private double[] potentialVector(EmployeeBase e) {
        return new double[]{
                e.performance(), levelScore(e.levelTier()), e.tenure(), e.skillCount(),
                e.trainingCount(), e.salaryCoefficient(), e.age()
        };
    }

    private Map<String, Double> attritionFeatures(EmployeeBase e) {
        return map(ATTRITION_FEATURES, attritionVector(e));
    }

    private Map<String, Double> potentialFeatures(EmployeeBase e) {
        return map(POTENTIAL_FEATURES, potentialVector(e));
    }

    private double[] positionFeatures(EmployeeBase employee, Set<Long> employeeSkills,
                                      PositionData position, Map<Long, Set<Long>> coreSkills,
                                      Map<Long, Set<Long>> plusSkills, PositionData currentPosition) {
        Set<Long> core = coreSkills.getOrDefault(position.id(), Set.of());
        Set<Long> plus = plusSkills.getOrDefault(position.id(), Set.of());
        return new double[]{
                coverage(employeeSkills, core),
                coverage(employeeSkills, plus),
                equalsText(employee.department(), position.department()),
                equalsText(employee.levelTier(), position.levelTier()),
                currentPosition != null
                        && equalsText(currentPosition.positionType(), position.positionType()) == 1 ? 1 : 0,
                employee.performance(),
                employee.potentialScore(),
                employee.tenure()
        };
    }

    private Map<String, Double> positionFeatureMap(EmployeeBase employee, Set<Long> employeeSkills,
                                                   PositionData position, Map<Long, Set<Long>> coreSkills,
                                                   Map<Long, Set<Long>> plusSkills,
                                                   PositionData currentPosition) {
        return map(POSITION_FEATURES, positionFeatures(
                employee, employeeSkills, position, coreSkills, plusSkills, currentPosition));
    }

    private double levelScore(String levelTier) {
        if (levelTier == null) return 0;
        return switch (levelTier) {
            case "决策层" -> 6;
            case "高管层" -> 5;
            case "总监层" -> 4;
            case "经理层" -> 3;
            case "主管层" -> 2;
            case "员工层" -> 1;
            default -> 0;
        };
    }

    private Map<Long, Set<Long>> positionSkillsMap() {
        return positionSkills("核心");
    }

    private double coverage(Set<Long> owned, Set<Long> required) {
        if (required.isEmpty()) return 0;
        long matched = required.stream().filter(owned::contains).count();
        return (double) matched / required.size();
    }

    private double equalsText(String left, String right) {
        return left != null && left.equals(right) ? 1 : 0;
    }

    private Map<String, Double> map(List<String> names, double[] values) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) result.put(names.get(i), values[i]);
        return result;
    }

    private Spec specOf(String code) {
        return switch (code) {
            case ATTRITION -> new Spec(ATTRITION, "人才流失风险预测", "分类模型");
            case POTENTIAL -> new Spec(POTENTIAL, "高潜人才识别", "分类模型");
            case POSITION_FIT -> new Spec(POSITION_FIT, "岗位匹配度推荐", "排序模型");
            default -> throw unsupported(code);
        };
    }

    private BusinessException unsupported(String code) {
        return new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                "未知模型：" + code);
    }

    private String nextVersion(String modelName) {
        int major = 1;
        int minor = -1;
        List<String> versions = jdbcTemplate.queryForList(
                "select version from model_log where model_name = ?", String.class, modelName);
        for (String version : versions) {
            Matcher matcher = VERSION_PATTERN.matcher(version == null ? "" : version);
            if (matcher.matches()) {
                int foundMajor = Integer.parseInt(matcher.group(1));
                int foundMinor = Integer.parseInt(matcher.group(2));
                if (foundMajor > major || (foundMajor == major && foundMinor > minor)) {
                    major = foundMajor;
                    minor = foundMinor;
                }
            }
        }
        return "v" + major + "." + (minor + 1);
    }

    private void persistArtifact(Artifact artifact) throws Exception {
        jdbcTemplate.update("""
                insert into model_artifact (
                  model_code, model_name, model_type, version, feature_names, means, stddevs,
                  coefficients, intercept, threshold, metrics, sample_count, positive_count,
                  dataset_version, train_time
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """,
                artifact.modelCode(), artifact.modelName(), artifact.modelType(), artifact.version(),
                json(artifact.features()), json(artifact.means()), json(artifact.stddevs()),
                json(artifact.coefficients()), artifact.intercept(), artifact.threshold(),
                json(artifact.metrics()), artifact.sampleCount(), artifact.positiveCount(),
                artifact.datasetVersion());
    }

    private void recordLog(Spec spec, Artifact artifact, String status, long duration,
                           Map<String, Object> extraParams, String remark) {
        Map<String, Object> params = new LinkedHashMap<>(extraParams);
        params.put("source", "live-training");
        params.put("modelCode", spec.code());
        try {
            jdbcTemplate.update("""
                    insert into model_log (
                      model_name, model_type, version, train_time, status, sample_count,
                      train_duration_ms, metrics, params, dataset_version, remark
                    ) values (?, ?, ?, now(), ?, ?, ?, ?, ?, ?, ?)
                    """,
                    spec.name(), spec.type(), artifact.version(), status, artifact.sampleCount(),
                    duration, json(artifact.metrics()), json(params), artifact.datasetVersion(), remark);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "模型日志写入失败：" + e.getMessage());
        }
    }

    private void recordFailedLog(Spec spec, String version, long duration, Exception error) {
        try {
            jdbcTemplate.update("""
                    insert into model_log (
                      model_name, model_type, version, train_time, status, sample_count,
                      train_duration_ms, metrics, params, dataset_version, remark
                    ) values (?, ?, ?, now(), '失败', 0, ?, '{}', ?, ?, ?)
                    """,
                    spec.name(), spec.type(), version, duration,
                    json(Map.of("source", "live-training", "modelCode", spec.code())),
                    datasetVersion, "训练失败：" + error.getMessage());
        } catch (Exception ignored) {
            // 保留原始训练异常。
        }
    }

    private Artifact loadArtifact(String code) {
        Artifact cached = artifactCache.get(code);
        if (cached != null) return cached;
        List<Artifact> artifacts = jdbcTemplate.query("""
                select id, model_code, model_name, model_type, version, feature_names,
                       means, stddevs, coefficients, intercept, threshold, metrics,
                       sample_count, positive_count, dataset_version,
                       date_format(train_time, '%Y-%m-%d %H:%i') as train_time_text
                from model_artifact
                where model_code = ?
                order by train_time desc, id desc
                limit 1
                """, (rs, rowNum) -> new Artifact(
                rs.getLong("id"),
                rs.getString("model_code"),
                rs.getString("model_name"),
                rs.getString("model_type"),
                rs.getString("version"),
                readList(rs.getString("feature_names")),
                readDoubleArray(rs.getString("means")),
                readDoubleArray(rs.getString("stddevs")),
                readDoubleArray(rs.getString("coefficients")),
                rs.getDouble("intercept"),
                rs.getDouble("threshold"),
                readMap(rs.getString("metrics")),
                rs.getInt("sample_count"),
                rs.getInt("positive_count"),
                rs.getString("dataset_version"),
                rs.getString("train_time_text")
        ), code);
        if (artifacts.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                    "模型尚未训练，请先在模型页面执行训练：" + code);
        }
        Artifact artifact = artifacts.get(0);
        artifactCache.put(code, artifact);
        return artifact;
    }

    private ModelTrainResultVO toTrainResult(Artifact artifact, long duration, String message) {
        ModelTrainResultVO vo = new ModelTrainResultVO();
        vo.setModelCode(artifact.modelCode());
        vo.setModelName(artifact.modelName());
        vo.setModelType(artifact.modelType());
        vo.setVersion(artifact.version());
        vo.setStatus("已完成");
        vo.setSampleCount(artifact.sampleCount());
        vo.setPositiveCount(artifact.positiveCount());
        vo.setTrainDurationMs(duration);
        vo.setFeatures(artifact.features());
        vo.setMetrics(artifact.metrics());
        vo.setMessage(message);
        return vo;
    }

    private Map<String, Object> trainingParams(List<String> features, String labelRule) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("algorithm", "StandardizedLogisticRegression");
        params.put("features", features);
        params.put("labelRule", labelRule);
        params.put("testRatio", 0.2);
        params.put("epochs", 1500);
        params.put("learningRate", 0.01);
        params.put("l2", 0.001);
        params.put("randomSeed", 20261027);
        return params;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "JSON 序列化失败");
        }
    }

    private List<String> readList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private double[] readDoubleArray(String json) {
        return readListNumbers(json).stream().mapToDouble(Double::doubleValue).toArray();
    }

    private List<Double> readListNumbers(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    private Number number(Object value) {
        return value instanceof Number number ? number : 0;
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private double round4(double value) {
        return Math.round(value * 10000) / 10000.0;
    }

    private record Spec(String code, String name, String type) { }

    private record Sample(double[] features, int label) { }

    private record PreparedTraining(List<String> featureNames, List<Sample> samples,
                                    Map<String, Object> params) { }

    private record EmployeeBase(
            long id, String empNo, String name, double age, double tenure, long positionId,
            String department, String levelTier, String workMode, double salaryCoefficient,
            double performance, double potentialScore, String potentialLevel,
            String indicators, int skillCount, int trainingCount, String warningLevel) { }

    private record PositionData(long id, String name, String department,
                                String levelTier, String positionType) { }

    private record Artifact(
            Long id, String modelCode, String modelName, String modelType, String version,
            List<String> features, double[] means, double[] stddevs, double[] coefficients,
            double intercept, double threshold, Map<String, Object> metrics,
            int sampleCount, int positiveCount, String datasetVersion, String trainTime) { }
}
