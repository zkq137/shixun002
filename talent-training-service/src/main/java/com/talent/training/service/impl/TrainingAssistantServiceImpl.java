package com.talent.training.service.impl;

import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import com.talent.training.dto.TrainingAssistantChatDTO;
import com.talent.training.service.TrainingAssistantService;
import com.talent.training.vo.TrainingAssistantReplyVO;
import com.talent.training.vo.TrainingAssistantStatusVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Dify 智能培训推荐工作流代理。
 *
 * <p>API Key 只放在服务端配置里，前端永远拿不到。
 */
@Service
@RequiredArgsConstructor
public class TrainingAssistantServiceImpl implements TrainingAssistantService {

    private static final Pattern REASONING_BLOCK = Pattern.compile(
            "(?is)<think\\b[^>]*>.*?</think\\s*>");

    private static final Pattern UNRESOLVED_TEMPLATE = Pattern.compile("\\{\\{#[^}]+#}}");

    private final TrainingAssistantContextService contextService;
    private final ObjectMapper objectMapper;

    @Value("${talent.dify.base-url:https://api.dify.ai}")
    private String baseUrl;

    @Value("${talent.dify.api-key:}")
    private String apiKey;

    @Value("${talent.dify.default-user:web-training}")
    private String defaultUser;

    private volatile RestClient restClient;

    @Override
    public TrainingAssistantStatusVO status() {
        TrainingAssistantStatusVO vo = new TrainingAssistantStatusVO();
        vo.setConfigured(isConfigured());
        vo.setBaseUrl(StringUtils.hasText(baseUrl) ? baseUrl : null);
        return vo;
    }

    @Override
    public TrainingAssistantReplyVO chat(TrainingAssistantChatDTO dto) {
        if (!isConfigured()) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "智能培训推荐助手尚未配置：请设置 TRAINING_DIFY_BASE_URL 和 "
                            + "TRAINING_DIFY_API_KEY 后重启培训服务");
        }

        String context = contextService.buildContext(dto.getQuery());
        Map<String, Object> body = new LinkedHashMap<>();
        // 培训证据由本服务读取并注入，Dify 不回调本地网关。
        body.put("inputs", Map.of(
                "query", dto.getQuery(),
                "training_context", context));
        body.put("query", dto.getQuery());
        body.put("response_mode", "streaming");
        body.put("conversation_id",
                dto.getConversationId() == null ? "" : dto.getConversationId());
        body.put("user", StringUtils.hasText(dto.getUser()) ? dto.getUser() : defaultUser);

        try {
            byte[] response = restClient()
                    .post()
                    .uri(endpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(body)
                    .retrieve()
                    .body(byte[].class);
            if (response == null || response.length == 0) {
                throw new BusinessException(ResultCode.FAIL.getCode(), "Dify 返回为空");
            }

            DifyStreamReply reply = parseStream(new String(response, StandardCharsets.UTF_8));
            TrainingAssistantReplyVO vo = new TrainingAssistantReplyVO();
            vo.setAnswer(cleanAnswer(resolveAnswer(reply)));
            vo.setConversationId(reply.conversationId);
            vo.setMessageId(reply.messageId);
            vo.setCreatedAt(reply.createdAt);
            if (!StringUtils.hasText(vo.getAnswer())
                    || UNRESOLVED_TEMPLATE.matcher(vo.getAnswer()).find()) {
                throw new BusinessException(ResultCode.FAIL.getCode(),
                        "Dify 工作流没有返回有效答复，请检查节点输出变量并重新发布应用");
            }
            return vo;
        } catch (RestClientResponseException e) {
            String detail = shorten(e.getResponseBodyAsString());
            if (detail.contains("502")) {
                detail += "。当前 Dify 应用可能仍是旧版 HTTP 工作流，"
                        + "请重新导入并发布 training_context 注入式工作流";
            }
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "Dify 调用失败（HTTP " + e.getStatusCode().value() + "）：" + detail);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "无法连接 Dify：" + e.getMessage());
        }
    }

    private DifyStreamReply parseStream(String raw) throws Exception {
        DifyStreamReply reply = new DifyStreamReply();
        StringBuilder answer = new StringBuilder();
        String finishedAnswer = null;

        boolean parsedEvent = false;
        for (String line : raw.split("\\R")) {
            String value = line.trim();
            int dataIndex = value.indexOf("data:");
            if (dataIndex < 0) {
                continue;
            }
            String payload = value.substring(dataIndex + "data:".length()).trim();
            if (!StringUtils.hasText(payload) || "[DONE]".equals(payload)) {
                continue;
            }
            JsonNode event = objectMapper.readTree(payload);
            String finished = readEvent(event, reply, answer, finishedAnswer);
            if (finished != null) {
                finishedAnswer = finished;
            }
            parsedEvent = true;
        }
        if (!parsedEvent) {
            String json = raw.startsWith("\uFEFF") ? raw.substring(1) : raw;
            finishedAnswer = readEvent(objectMapper.readTree(json.trim()), reply, answer, null);
            reply.finishedAnswer = finishedAnswer;
        }

        reply.answer = answer.isEmpty() ? finishedAnswer : answer.toString();
        if (reply.promptUnresolved) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "Dify 工作流节点变量未解析，请重新导入并发布 dify/talent-training-agent.yml");
        }
        return reply;
    }

    private String readEvent(JsonNode event, DifyStreamReply reply, StringBuilder answer,
                             String currentFinishedAnswer) {
        readEnvelope(event, reply);
        String type = event.path("event").asText();
        if ("error".equals(type)) {
            String detail = event.path("message").asText("Dify 工作流执行失败");
            throw new BusinessException(ResultCode.FAIL.getCode(), "Dify 调用失败：" + shorten(detail));
        }

        JsonNode data = event.path("data");
        String finishedAnswer = currentFinishedAnswer;
        if ("message".equals(type) || !event.has("event")) {
            String chunk = event.path("answer").asText("");
            if (StringUtils.hasText(chunk)) {
                answer.append(chunk);
            }
        } else if ("node_finished".equals(type)
                && "llm".equals(data.path("node_type").asText())
                && "succeeded".equals(data.path("status").asText())) {
            String text = data.path("outputs").path("text").asText("");
            if (StringUtils.hasText(text)) {
                reply.llmAnswer = text;
            }
            String prompts = data.path("process_data").path("prompts").toString();
            if (UNRESOLVED_TEMPLATE.matcher(prompts).find()) {
                reply.promptUnresolved = true;
            }
        } else if ("workflow_finished".equals(type)) {
            String status = data.path("status").asText();
            if (!"succeeded".equals(status)) {
                String detail = data.path("error").asText("工作流执行失败");
                throw new BusinessException(ResultCode.FAIL.getCode(), "Dify 调用失败：" + shorten(detail));
            }
            String output = data.path("outputs").path("answer").asText("");
            if (StringUtils.hasText(output)) {
                finishedAnswer = output;
                reply.finishedAnswer = output;
            }
        }
        return finishedAnswer;
    }

    private void readEnvelope(JsonNode event, DifyStreamReply reply) {
        String conversationId = event.path("conversation_id").asText("");
        if (StringUtils.hasText(conversationId)) {
            reply.conversationId = conversationId;
        }
        String messageId = event.path("message_id").asText("");
        if (StringUtils.hasText(messageId)) {
            reply.messageId = messageId;
        }
        long createdAt = event.path("created_at").asLong(0);
        if (createdAt > 0) {
            reply.createdAt = createdAt;
        }
    }

    private String resolveAnswer(DifyStreamReply reply) {
        if (StringUtils.hasText(reply.answer)
                && !UNRESOLVED_TEMPLATE.matcher(reply.answer).find()) {
            return reply.answer;
        }
        return StringUtils.hasText(reply.llmAnswer) ? reply.llmAnswer : reply.answer;
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

    private static final class DifyStreamReply {
        private String answer;
        private String llmAnswer;
        private String finishedAnswer;
        private String conversationId;
        private String messageId;
        private Long createdAt;
        private boolean promptUnresolved;
    }
}
