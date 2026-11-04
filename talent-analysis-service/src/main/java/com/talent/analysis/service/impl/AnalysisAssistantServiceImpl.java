package com.talent.analysis.service.impl;

import com.talent.analysis.dto.AnalysisAssistantChatDTO;
import com.talent.analysis.service.FlowForecastService;
import com.talent.analysis.service.AnalysisAssistantService;
import com.talent.analysis.service.TalentModelService;
import com.talent.analysis.vo.AnalysisAssistantReplyVO;
import com.talent.analysis.vo.AnalysisAssistantStatusVO;
import com.talent.analysis.vo.FlowForecastVO;
import com.talent.analysis.vo.ModelPredictionVO;
import com.talent.analysis.vo.ModelTrainResultVO;
import com.talent.analysis.vo.PositionRecommendationVO;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据分析模块的 Dify 服务端代理。
 */
@Service
@RequiredArgsConstructor
public class AnalysisAssistantServiceImpl implements AnalysisAssistantService {

    private final JdbcTemplate jdbcTemplate;
    private final FlowForecastService flowForecastService;
    private final TalentModelService talentModelService;
    private final AnalysisEvidenceService evidenceService;

    private static final Pattern REASONING_BLOCK = Pattern.compile(
            "(?is)<think\\b[^>]*>.*?</think\\s*>");
    private static final Pattern HORIZON_PATTERN = Pattern.compile("(\\d{1,2})\\s*个月");
    private static final Pattern TOP_N_PATTERN = Pattern.compile("(\\d{1,2})\\s*个");
    private static final Pattern NUMERIC_ID_PATTERN = Pattern.compile(
            "(?:员工\\s*ID|ID|编号|员工)\\s*[:：]?\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ONLINE_INFERENCE_NAME_PATTERN = Pattern.compile(
            "(?:对|给|为|员工|姓名\\s*[:：]?)\\s*([\\u4e00-\\u9fa5]{2,4})\\s*(?:进行|做)?\\s*在线推理");

    @Value("${talent.dify.base-url:}")
    private String baseUrl;

    @Value("${talent.dify.api-key:}")
    private String apiKey;

    @Value("${talent.dify.default-user:web-analysis}")
    private String defaultUser;

    private volatile RestClient restClient;

    @Override
    public AnalysisAssistantStatusVO status() {
        AnalysisAssistantStatusVO vo = new AnalysisAssistantStatusVO();
        vo.setConfigured(isConfigured());
        vo.setBaseUrl(StringUtils.hasText(baseUrl) ? baseUrl : null);
        return vo;
    }

    @Override
    public AnalysisAssistantReplyVO chat(AnalysisAssistantChatDTO dto) {
        String directAction = executeDirectAction(dto.getQuery());
        if (directAction != null) {
            AnalysisAssistantReplyVO vo = new AnalysisAssistantReplyVO();
            vo.setAnswer(directAction);
            vo.setMessageId(UUID.randomUUID().toString());
            vo.setCreatedAt(System.currentTimeMillis());
            return vo;
        }

        if (!isConfigured()) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "数据分析智能体尚未配置 ANALYSIS_DIFY_BASE_URL 和 ANALYSIS_DIFY_API_KEY");
        }

        String context = evidenceService.buildContext(dto.getQuery());
        Map<String, Object> body = new LinkedHashMap<>();
        // The backend injects a read-only analytics snapshot; Dify does not call the gateway.
        body.put("inputs", Map.of("query", dto.getQuery(), "analysis_context", context));
        body.put("query", dto.getQuery());
        body.put("response_mode", "blocking");
        body.put("conversation_id",
                dto.getConversationId() == null ? "" : dto.getConversationId());
        body.put("user", StringUtils.hasText(dto.getUser()) ? dto.getUser() : defaultUser);

        try {
            Map<String, Object> response = restClient()
                    .post()
                    .uri(endpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() { });

            if (response == null) {
                throw new BusinessException(ResultCode.FAIL.getCode(), "Dify 返回为空");
            }

            AnalysisAssistantReplyVO vo = new AnalysisAssistantReplyVO();
            vo.setAnswer(cleanAnswer(stringValue(response.get("answer"))));
            vo.setConversationId(stringValue(response.get("conversation_id")));
            vo.setMessageId(stringValue(response.get("message_id")));
            Object createdAt = response.get("created_at");
            if (createdAt instanceof Number number) {
                vo.setCreatedAt(number.longValue());
            }
            if (!StringUtils.hasText(vo.getAnswer())) {
                throw new BusinessException(ResultCode.FAIL.getCode(),
                        "Dify 没有返回分析结论");
            }
            return vo;
        } catch (RestClientResponseException e) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "数据分析智能体调用失败（HTTP " + e.getStatusCode().value() + "）："
                            + shorten(e.getResponseBodyAsString()));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "无法连接数据分析智能体：" + e.getMessage());
        }
    }

    private boolean isConfigured() {
        return StringUtils.hasText(baseUrl) && StringUtils.hasText(apiKey);
    }

    /**
     * 常见分析请求的接口是确定的。给规划模型注入硬约束，避免它偶尔错误返回 NONE。
     */
    private String hardenAnalysisIntent(String query) {
        String plan = deterministicActionPlan(query);
        if (plan != null) {
            return query + "\n[系统预校验：该需求已经匹配到唯一接口。"
                    + "禁止返回 NONE，必须原样输出以下调用计划：" + plan + "]";
        }
        if (query.contains("健康度") || query.contains("梯队整体状态")) {
            plan = "{\"method\":\"GET\",\"path\":\"/api/analysis/pipeline\","
                    + "\"body\":{},\"summary\":\"查询人才梯队整体健康度\"}";
        } else if (query.contains("流动")
                || query.contains("流入")
                || query.contains("流出")
                || query.contains("离职趋势")
                || query.contains("晋升趋势")) {
            plan = "{\"method\":\"GET\",\"path\":\"/api/analysis/flow-trend\","
                    + "\"body\":{},\"summary\":\"查询人才历史流动趋势\"}";
        } else if (query.contains("模型效果")
                || query.contains("模型运行")
                || query.contains("模型表现")
                || query.contains("成功率")) {
            plan = "{\"method\":\"GET\",\"path\":\"/api/analysis/model/effect\","
                    + "\"body\":{},\"summary\":\"查询模型运行效果\"}";
        } else if (query.contains("模型日志")
                || query.contains("训练日志")
                || (query.contains("日志") && query.contains("查询"))) {
            plan = "{\"method\":\"GET\","
                    + "\"path\":\"/api/analysis/model/logs?pageNum=1&pageSize=10\","
                    + "\"body\":{},\"summary\":\"查询模型训练日志\"}";
        } else if (query.contains("报表选项")
                || (query.contains("维度") && query.contains("指标"))) {
            plan = "{\"method\":\"GET\",\"path\":\"/api/analysis/report/options\","
                    + "\"body\":{},\"summary\":\"查询报表可选维度和指标\"}";
        } else if (query.contains("报表模板")
                && (query.contains("查询") || query.contains("列表") || query.contains("查看"))) {
            plan = "{\"method\":\"GET\",\"path\":\"/api/analysis/report/templates\","
                    + "\"body\":{},\"summary\":\"查询报表模板\"}";
        } else if (query.contains("历史报表") || query.contains("报表记录")) {
            plan = "{\"method\":\"GET\",\"path\":\"/api/analysis/report/history\","
                    + "\"body\":{},\"summary\":\"查询历史报表\"}";
        } else if (query.contains("盘点") || query.contains("基础看板")
                || query.contains("部门分布")) {
            plan = "{\"method\":\"GET\",\"path\":\"/api/analysis/dashboard\","
                    + "\"body\":{},\"summary\":\"查询人才盘点看板\"}";
        }
        if (plan == null) {
            return query;
        }
        return query + "\n[系统预校验：该需求已经匹配到唯一接口。"
                + "禁止返回 NONE，必须原样输出以下调用计划：" + plan + "]";
    }

    private String deterministicActionPlan(String query) {
        if (isFlowForecastQuery(query)) {
            int horizon = horizon(query, 6);
            return "{\"method\":\"POST\","
                    + "\"path\":\"/api/analysis/forecast/generate?horizonMonths=" + horizon + "\","
                    + "\"body\":{},\"summary\":\"生成未来 " + horizon + " 个月人才流入流出晋升预测\"}";
        }

        if (query.contains("模型")
                && (query.contains("训练") || query.contains("重训"))) {
            String path;
            String summary;
            if (query.contains("流失")) {
                path = "/api/analysis/model/train/attrition-risk";
                summary = "重新训练人才流失风险模型";
            } else if (query.contains("高潜") || query.contains("潜力")) {
                path = "/api/analysis/model/train/high-potential";
                summary = "重新训练高潜人才识别模型";
            } else if (query.contains("岗位") || query.contains("匹配")) {
                path = "/api/analysis/model/train/position-fit";
                summary = "重新训练岗位匹配模型";
            } else {
                path = "/api/analysis/model/train-all";
                summary = "训练全部人才模型";
            }
            return "{\"method\":\"POST\",\"path\":\"" + path
                    + "\",\"body\":{},\"summary\":\"" + summary + "\"}";
        }

        Long employeeId = resolveEmployeeReference(query);
        if (employeeId != null && query.contains("高潜")
                && (query.contains("预测") || query.contains("识别") || query.contains("推理"))) {
            return "{\"method\":\"GET\",\"path\":\"/api/analysis/model/predict/potential/"
                    + employeeId + "\",\"body\":{},\"summary\":\"预测员工高潜等级\"}";
        }
        if (employeeId != null && query.contains("岗位")
                && (query.contains("推荐") || query.contains("匹配"))) {
            return "{\"method\":\"GET\",\"path\":\"/api/analysis/model/recommend/positions/"
                    + employeeId + "?topN=" + topN(query, 5)
                    + "\",\"body\":{},\"summary\":\"推荐匹配岗位\"}";
        }
        if (employeeId != null && query.contains("流失") && query.contains("风险")
                && (query.contains("预测") || query.contains("推理"))) {
            return "{\"method\":\"GET\",\"path\":\"/api/analysis/model/predict/attrition/"
                    + employeeId + "\",\"body\":{},\"summary\":\"预测员工流失风险\"}";
        }
        return null;
    }

    /**
     * 已发布 Dify 工作流提示词可能滞后。明确的预测、训练、推理操作由服务端直接执行，
     * 保证功能可用；普通开放分析仍走 Dify。
     */
    private String executeDirectAction(String query) {
        if (isFlowForecastQuery(query)) {
            int horizon = horizon(query, 6);
            return formatForecast(flowForecastService.generate(horizon));
        }

        if (query.contains("模型")
                && (query.contains("训练") || query.contains("重训"))) {
            List<ModelTrainResultVO> results;
            if (query.contains("流失")) {
                results = List.of(talentModelService.train("attrition-risk"));
            } else if (query.contains("高潜") || query.contains("潜力")) {
                results = List.of(talentModelService.train("high-potential"));
            } else if (query.contains("岗位") || query.contains("匹配")) {
                results = List.of(talentModelService.train("position-fit"));
            } else {
                results = talentModelService.trainAll();
            }
            return formatTraining(results);
        }

        Long employeeId = resolveEmployeeReference(query);
        if (employeeId == null && isEmployeeSpecificQuery(query)) {
            return employeeNotFoundReply(query);
        }
        if (employeeId != null && query.contains("在线推理")) {
            String identifier = String.valueOf(employeeId);
            return formatPrediction(talentModelService.predictAttrition(identifier))
                    + "\n\n" + formatPrediction(talentModelService.predictHighPotential(identifier))
                    + "\n\n" + formatRecommendations(
                    talentModelService.recommendPositions(identifier, 5));
        }
        if (employeeId != null && query.contains("高潜")
                && (query.contains("预测") || query.contains("识别") || query.contains("推理"))) {
            return formatPrediction(talentModelService.predictHighPotential(
                    String.valueOf(employeeId)));
        }
        if (employeeId != null && query.contains("岗位")
                && (query.contains("推荐") || query.contains("匹配"))) {
            return formatRecommendations(talentModelService.recommendPositions(
                    String.valueOf(employeeId), topN(query, 5)));
        }
        if (employeeId != null && query.contains("流失") && query.contains("风险")
                && (query.contains("预测") || query.contains("推理"))) {
            return formatPrediction(talentModelService.predictAttrition(
                    String.valueOf(employeeId)));
        }
        return null;
    }

    private boolean isEmployeeSpecificQuery(String query) {
        if (query.contains("在线推理")) {
            return true;
        }
        boolean employeeIntent = query.contains("员工") || query.contains("姓名");
        boolean predictionIntent = query.contains("预测")
                || query.contains("推理")
                || query.contains("识别")
                || query.contains("推荐")
                || query.contains("匹配");
        boolean employeeTopic = query.contains("流失")
                || query.contains("离职")
                || query.contains("高潜")
                || query.contains("潜力")
                || query.contains("岗位");
        return employeeIntent && predictionIntent && employeeTopic;
    }

    private String employeeNotFoundReply(String query) {
        List<EmployeeCandidate> employees = jdbcTemplate.query("""
                select id, emp_no, name, department
                from emp_employee
                where status = '在职'
                """, (rs, rowNum) -> new EmployeeCandidate(
                rs.getLong("id"),
                rs.getString("emp_no"),
                rs.getString("name"),
                rs.getString("department"),
                0));

        StringBuilder text = new StringBuilder();
        String employeeName = onlineInferenceEmployeeName(query);
        text.append(StringUtils.hasText(employeeName)
                        ? "未找到员工“" + employeeName + "”，无法执行在线推理。\n"
                        : "未找到与本次请求匹配的在职员工，无法执行在线推理。\n")
                .append("当前在职员工 ").append(employees.size())
                .append(" 人，可能是姓名有误或该员工已离职。\n");

        if (!query.contains("在线推理")) {
            text.append("\n请提供员工的数字 ID、完整工号或完整姓名后重试。");
            return text.toString().trim();
        }

        List<EmployeeCandidate> candidates = employees.stream()
                .map(employee -> new EmployeeCandidate(
                        employee.id(),
                        employee.empNo(),
                        employee.name(),
                        employee.department(),
                        longestCommonSubstring(query, employee.name())))
                .filter(employee -> employee.similarity() >= 2)
                .sorted(Comparator.comparingInt(EmployeeCandidate::similarity).reversed()
                        .thenComparingLong(EmployeeCandidate::id))
                .limit(3)
                .toList();
        if (candidates.isEmpty()) {
            text.append("\n请确认员工姓名，或改用员工数字 ID / 工号发送在线推理请求。");
            return text.toString().trim();
        }

        text.append("\n最相近的员工：\n");
        for (EmployeeCandidate employee : candidates) {
            text.append("• ").append(employee.name())
                    .append("（ID ").append(employee.id())
                    .append("，工号 ").append(employee.empNo())
                    .append("，").append(employee.department()).append("）\n");
        }
        EmployeeCandidate first = candidates.get(0);
        text.append("\n如为姓名笔误，请确认后发送：对")
                .append(first.name()).append("进行在线推理。");
        return text.toString().trim();
    }

    private String onlineInferenceEmployeeName(String query) {
        Matcher matcher = ONLINE_INFERENCE_NAME_PATTERN.matcher(query);
        return matcher.find() ? matcher.group(1) : null;
    }

    private int longestCommonSubstring(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return 0;
        }
        int[] previous = new int[right.length() + 1];
        int longest = 0;
        for (int i = 1; i <= left.length(); i++) {
            int[] current = new int[right.length() + 1];
            for (int j = 1; j <= right.length(); j++) {
                if (left.charAt(i - 1) == right.charAt(j - 1)) {
                    current[j] = previous[j - 1] + 1;
                    longest = Math.max(longest, current[j]);
                }
            }
            previous = current;
        }
        return longest;
    }

    private record EmployeeCandidate(
            long id, String empNo, String name, String department, int similarity) {
    }

    private boolean isFlowForecastQuery(String query) {
        boolean predictIntent = query.contains("预测") || query.contains("趋势预测");
        boolean flowTopic = query.contains("流动")
                || query.contains("流入")
                || query.contains("流出")
                || query.contains("离职趋势")
                || (query.contains("晋升") && query.contains("趋势"));
        return predictIntent && flowTopic;
    }

    private String formatForecast(FlowForecastVO forecast) {
        StringBuilder text = new StringBuilder();
        text.append("已完成人才流动趋势预测\n")
                .append("预测周期：未来 ").append(forecast.getHorizonMonths()).append(" 个月\n")
                .append("预测批次：").append(forecast.getRunCode()).append("\n\n");
        for (FlowForecastVO.Series series : forecast.getSeries()) {
            text.append(series.getName()).append("：预计合计 ")
                    .append(series.getTotalPredicted()).append(" 人，")
                    .append(series.getConfidence()).append("置信度\n");
            for (FlowForecastVO.Point point : series.getPoints()) {
                text.append("• ").append(point.getMonth()).append("：")
                        .append(point.getPredictedValue()).append(" 人，区间 ")
                        .append(point.getLowerBound()).append("—")
                        .append(point.getUpperBound()).append("\n");
            }
            text.append("方法：").append(friendlyMethod(series.getMethod())).append("\n\n");
        }
        text.append("说明：");
        text.append(String.join("；", forecast.getNotes()));
        return text.toString().trim();
    }

    private String friendlyMethod(String method) {
        return switch (method) {
            case "seasonal-linear-trend" -> "季节调整线性趋势";
            default -> method;
        };
    }

    private String formatTraining(List<ModelTrainResultVO> results) {
        StringBuilder text = new StringBuilder("模型训练完成\n\n");
        for (ModelTrainResultVO result : results) {
            text.append("• ").append(result.getModelName()).append(" ")
                    .append(result.getVersion()).append("：")
                    .append(result.getStatus()).append("，样本 ")
                    .append(result.getSampleCount()).append(" 条，AUC ")
                    .append(result.getMetrics().getOrDefault("auc", "-"))
                    .append("，F1 ")
                    .append(result.getMetrics().getOrDefault("f1", "-"))
                    .append("\n");
        }
        text.append("\n训练参数、版本和指标已写入模型日志，最新模型已可直接在线推理。");
        return text.toString();
    }

    private String formatPrediction(ModelPredictionVO prediction) {
        StringBuilder text = new StringBuilder();
        text.append(prediction.getModelName()).append("结果\n")
                .append("员工：").append(prediction.getEmployeeName())
                .append("（ID ").append(prediction.getEmployeeId()).append("）\n")
                .append("模型版本：").append(prediction.getModelVersion()).append("\n")
                .append("预测结论：").append(prediction.getResultLabel()).append("\n")
                .append("预测分数：").append(prediction.getScorePercent()).append(" 分\n")
                .append(prediction.getExplanation()).append("\n");
        if (!prediction.getTopFeatures().isEmpty()) {
            text.append("主要影响因素：\n");
            prediction.getTopFeatures().forEach((feature, contribution) ->
                    text.append("• ").append(feature).append("：")
                            .append(contribution >= 0 ? "提高概率 " : "降低概率 ")
                            .append(contribution).append("\n"));
        }
        return text.toString().trim();
    }

    private String formatRecommendations(PositionRecommendationVO recommendation) {
        StringBuilder text = new StringBuilder();
        text.append("岗位匹配推荐\n")
                .append("员工：").append(recommendation.getEmployeeName())
                .append("（ID ").append(recommendation.getEmployeeId()).append("）\n")
                .append("模型版本：").append(recommendation.getModelVersion()).append("\n\n");
        for (PositionRecommendationVO.Item item : recommendation.getRecommendations()) {
            text.append("• ").append(item.getPositionName())
                    .append("（").append(item.getDepartment()).append("）：")
                    .append(item.getScore()).append(" 分\n");
        }
        return text.toString().trim();
    }

    private int horizon(String query, int fallback) {
        Matcher matcher = HORIZON_PATTERN.matcher(query);
        return matcher.find() ? Math.max(1, Math.min(12, Integer.parseInt(matcher.group(1)))) : fallback;
    }

    private int topN(String query, int fallback) {
        Matcher matcher = TOP_N_PATTERN.matcher(query);
        return matcher.find() ? Math.max(1, Math.min(20, Integer.parseInt(matcher.group(1)))) : fallback;
    }

    private Long resolveEmployeeReference(String query) {
        Matcher idMatcher = NUMERIC_ID_PATTERN.matcher(query);
        if (idMatcher.find()) {
            return Long.parseLong(idMatcher.group(1));
        }

        List<Map<String, Object>> candidates = jdbcTemplate.queryForList("""
                select id, emp_no, name
                from emp_employee
                where ? like concat('%', name, '%')
                   or ? like concat('%', emp_no, '%')
                order by char_length(name) desc, id
                limit 20
                """, query, query);
        if (candidates.isEmpty()) {
            return null;
        }
        int maxNameLength = candidates.stream()
                .mapToInt(row -> String.valueOf(row.get("name")).length())
                .max()
                .orElse(0);
        List<Map<String, Object>> longest = candidates.stream()
                .filter(row -> String.valueOf(row.get("name")).length() == maxNameLength)
                .toList();
        if (longest.size() > 1) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "姓名不唯一，请补充员工工号或数字ID");
        }
        return ((Number) longest.get(0).get("id")).longValue();
    }

    private RestClient restClient() {
        RestClient current = restClient;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (restClient == null) {
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                requestFactory.setConnectTimeout(10_000);
                requestFactory.setReadTimeout(300_000);
                restClient = RestClient.builder().requestFactory(requestFactory).build();
            }
            return restClient;
        }
    }

    private String endpoint() {
        String base = baseUrl.trim().replaceAll("/+$", "");
        return base.endsWith("/v1") ? base + "/chat-messages" : base + "/v1/chat-messages";
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String cleanAnswer(String answer) {
        if (!StringUtils.hasText(answer)) return answer;
        String cleaned = REASONING_BLOCK.matcher(answer).replaceAll("").trim();
        String[] lines = cleaned.split("\\R");
        int start = 0;
        while (start < lines.length) {
            String line = lines[start].trim();
            if (line.isEmpty()
                    || line.startsWith("[系统预校验")
                    || line.startsWith("调用计划：")
                    || line.startsWith("{\"method\":")) {
                start++;
                continue;
            }
            break;
        }
        if (start > 0 && start < lines.length) {
            cleaned = String.join("\n", java.util.Arrays.copyOfRange(lines, start, lines.length)).trim();
        }
        return cleaned;
    }

    private String shorten(String text) {
        if (!StringUtils.hasText(text)) {
            return "无响应内容";
        }
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 500 ? oneLine : oneLine.substring(0, 500) + "...";
    }
}
