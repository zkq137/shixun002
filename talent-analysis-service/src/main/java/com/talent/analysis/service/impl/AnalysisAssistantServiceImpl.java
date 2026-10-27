package com.talent.analysis.service.impl;

import com.talent.analysis.dto.AnalysisAssistantChatDTO;
import com.talent.analysis.service.AnalysisAssistantService;
import com.talent.analysis.vo.AnalysisAssistantReplyVO;
import com.talent.analysis.vo.AnalysisAssistantStatusVO;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据分析模块的 Dify 服务端代理。
 */
@Service
@RequiredArgsConstructor
public class AnalysisAssistantServiceImpl implements AnalysisAssistantService {

    private static final Pattern REASONING_BLOCK = Pattern.compile(
            "(?is)<think\\b[^>]*>.*?</think\\s*>");

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
        if (!isConfigured()) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "数据分析智能体尚未配置 ANALYSIS_DIFY_BASE_URL 和 ANALYSIS_DIFY_API_KEY");
        }

        String query = hardenAnalysisIntent(dto.getQuery());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", Map.of("query", query));
        body.put("query", query);
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
        String plan = null;
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

    private RestClient restClient() {
        RestClient current = restClient;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (restClient == null) {
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                requestFactory.setConnectTimeout(10_000);
                requestFactory.setReadTimeout(180_000);
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
