package com.talent.promotion.service.impl;

import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import com.talent.promotion.dto.AssistantChatDTO;
import com.talent.promotion.service.PromotionAssistantService;
import com.talent.promotion.vo.AssistantReplyVO;
import com.talent.promotion.vo.AssistantStatusVO;
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
 * Dify 晋升决策工作流代理。
 *
 * <p>API Key 只放在服务端配置里，前端永远拿不到。
 * DIFY_BASE_URL 可以填 Dify 根地址或带 /v1 的 API 地址。
 */
@Service
@RequiredArgsConstructor
public class PromotionAssistantServiceImpl implements PromotionAssistantService {

    private static final Pattern REASONING_BLOCK = Pattern.compile(
            "(?is)<think\\b[^>]*>.*?</think\\s*>");

    private final PromotionEvidenceService evidenceService;

    @Value("${talent.dify.base-url:}")
    private String baseUrl;

    @Value("${talent.dify.api-key:}")
    private String apiKey;

    @Value("${talent.dify.default-user:web-promotion}")
    private String defaultUser;

    private volatile RestClient restClient;

    @Override
    public AssistantStatusVO status() {
        AssistantStatusVO vo = new AssistantStatusVO();
        vo.setConfigured(isConfigured());
        vo.setBaseUrl(StringUtils.hasText(baseUrl) ? baseUrl : null);
        return vo;
    }

    @Override
    public AssistantReplyVO chat(AssistantChatDTO dto) {
        if (!isConfigured()) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "晋升 AI 助手尚未配置：请设置 PROMOTION_DIFY_BASE_URL 和 "
                            + "PROMOTION_DIFY_API_KEY 后重启晋升服务");
        }

        String context = evidenceService.buildContext(dto.getQuery());
        Map<String, Object> body = new LinkedHashMap<>();
        // 数据由本服务读取并注入，Dify 工作流不再访问本机接口。
        body.put("inputs", Map.of(
                "query", dto.getQuery(),
                "promotion_context", context));
        body.put("query", dto.getQuery());
        body.put("response_mode", "blocking");
        body.put("conversation_id", dto.getConversationId() == null ? "" : dto.getConversationId());
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
            AssistantReplyVO vo = new AssistantReplyVO();
            vo.setAnswer(cleanAnswer(stringValue(response.get("answer"))));
            vo.setConversationId(stringValue(response.get("conversation_id")));
            vo.setMessageId(stringValue(response.get("message_id")));
            Object createdAt = response.get("created_at");
            if (createdAt instanceof Number number) {
                vo.setCreatedAt(number.longValue());
            }
            if (!StringUtils.hasText(vo.getAnswer())) {
                throw new BusinessException(ResultCode.FAIL.getCode(),
                        "Dify 没有返回 answer，请确认晋升应用是 Chatflow（advanced-chat）并已发布");
            }
            return vo;
        } catch (RestClientResponseException e) {
            String detail = shorten(e.getResponseBodyAsString());
            if (detail.contains("502")) {
                detail += "。当前 Dify 应用可能仍是旧版 HTTP 工作流，"
                        + "请重新导入并发布 promotion_context 注入式工作流";
            }
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "Dify 调用失败（HTTP " + e.getStatusCode().value() + "）："
                            + detail);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "无法连接 Dify：" + e.getMessage());
        }
    }

    private boolean isConfigured() {
        return StringUtils.hasText(baseUrl) && StringUtils.hasText(apiKey);
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
                requestFactory.setReadTimeout(120_000);
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
        return StringUtils.hasText(answer)
                ? REASONING_BLOCK.matcher(answer).replaceAll("").trim()
                : answer;
    }

    private String shorten(String text) {
        if (!StringUtils.hasText(text)) {
            return "无响应内容";
        }
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 300 ? oneLine : oneLine.substring(0, 300) + "...";
    }
}
