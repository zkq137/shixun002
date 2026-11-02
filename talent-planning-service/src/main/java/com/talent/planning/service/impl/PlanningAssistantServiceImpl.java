package com.talent.planning.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import com.talent.planning.dto.PlanningAssistantChatDTO;
import com.talent.planning.service.PlanningAssistantService;
import com.talent.planning.service.PlanningService;
import com.talent.planning.vo.PlanningAssistantReplyVO;
import com.talent.planning.vo.PlanningAssistantStatusVO;
import com.talent.planning.vo.PositionVO;
import com.talent.planning.vo.SuccessionCandidateVO;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 在本地读取规划数据，再把脱敏后的业务上下文交给 Dify 网页版解读。
 * Dify 不需要、也不能直接访问本机网关。
 */
@Service
@RequiredArgsConstructor
public class PlanningAssistantServiceImpl implements PlanningAssistantService {

    private static final Pattern POSITION_ID_PATTERN = Pattern.compile(
            "(?:岗位\\s*ID|岗位编号)\\s*[:：]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REASONING_BLOCK = Pattern.compile(
            "(?is)<think\\b[^>]*>.*?</think\\s*>");
    private static final int MAX_CONTEXT_LENGTH = 48_000;

    private final PlanningService planningService;
    private final ObjectMapper objectMapper;

    @Value("${talent.dify.base-url:https://api.dify.ai}")
    private String baseUrl;

    @Value("${talent.dify.api-key:}")
    private String apiKey;

    @Value("${talent.dify.default-user:web-planning}")
    private String defaultUser;

    private volatile RestClient restClient;

    @Override
    public PlanningAssistantStatusVO status() {
        PlanningAssistantStatusVO vo = new PlanningAssistantStatusVO();
        vo.setConfigured(isConfigured());
        vo.setBaseUrl(StringUtils.hasText(baseUrl) ? baseUrl : null);
        return vo;
    }

    @Override
    public PlanningAssistantReplyVO chat(PlanningAssistantChatDTO dto) {
        if (!isConfigured()) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "梯队规划智能体尚未配置 PLANNING_DIFY_API_KEY");
        }

        String context = buildContext(dto.getQuery());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", Map.of(
                "query", dto.getQuery(),
                "planning_context", context));
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

            PlanningAssistantReplyVO vo = new PlanningAssistantReplyVO();
            vo.setAnswer(cleanAnswer(stringValue(response.get("answer"))));
            vo.setConversationId(stringValue(response.get("conversation_id")));
            vo.setMessageId(stringValue(response.get("message_id")));
            Object createdAt = response.get("created_at");
            if (createdAt instanceof Number number) {
                vo.setCreatedAt(number.longValue());
            }
            if (!StringUtils.hasText(vo.getAnswer())) {
                throw new BusinessException(ResultCode.FAIL.getCode(), "Dify 没有返回分析结论");
            }
            return vo;
        } catch (RestClientResponseException e) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "梯队规划智能体调用失败（HTTP " + e.getStatusCode().value() + "）："
                            + shorten(e.getResponseBodyAsString()));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "无法连接梯队规划智能体：" + e.getMessage());
        }
    }

    String buildContext(String query) {
        List<PositionVO> positions = planningService.positions(null, null);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("generatedAt", LocalDateTime.now());
        context.put("privacy", "员工姓名、工号和数据库ID未发送；候选人使用顺序编号");
        context.put("dashboard", planningService.dashboard());
        context.put("keyPositions", sanitizePositions(
                positions.stream().filter(position -> Boolean.TRUE.equals(position.getKey()))
                        .limit(30).toList()));
        context.put("talentPools", planningService.talentPools());
        context.put("positionRisks", planningService.positionRisks().stream().limit(30).toList());

        PositionVO matched = resolvePosition(query, positions);
        if (matched != null) {
            PositionVO detail = planningService.positionDetail(matched.getId());
            Map<String, Object> positionContext = new LinkedHashMap<>();
            positionContext.put("position", sanitizePosition(detail));
            positionContext.put("skillCoverage", planningService.skillCoverage(matched.getId()));
            positionContext.put("candidates", sanitizeCandidates(
                    planningService.succession(matched.getId(), 20)));
            context.put("matchedPosition", positionContext);
        } else {
            context.put("positionDirectory", sanitizePositions(positions.stream().limit(100).toList()));
            context.put("hint", "若需分析单个岗位，请在问题中写出完整岗位名称或岗位ID");
        }

        try {
            String json = objectMapper.writeValueAsString(context);
            return json.length() <= MAX_CONTEXT_LENGTH
                    ? json
                    : json.substring(0, MAX_CONTEXT_LENGTH)
                    + "\n[上下文过长，已在本地截断]";
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "规划上下文序列化失败");
        }
    }

    private PositionVO resolvePosition(String query, List<PositionVO> positions) {
        Matcher matcher = POSITION_ID_PATTERN.matcher(query);
        if (matcher.find()) {
            long id = Long.parseLong(matcher.group(1));
            return positions.stream().filter(position -> position.getId() == id).findFirst().orElse(null);
        }
        return positions.stream()
                .filter(position -> StringUtils.hasText(position.getPositionName())
                        && query.contains(position.getPositionName()))
                .max(Comparator.comparingInt(position -> position.getPositionName().length()))
                .orElse(null);
    }

    private List<Map<String, Object>> sanitizePositions(List<PositionVO> positions) {
        return positions.stream().map(this::sanitizePosition).toList();
    }

    private Map<String, Object> sanitizePosition(PositionVO position) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("positionName", position.getPositionName());
        item.put("positionLevel", position.getPositionLevel());
        item.put("key", position.getKey());
        item.put("incumbentCount", position.getIncumbentCount());
        item.put("successorCount", position.getSuccessorCount());
        item.put("readyNowCount", position.getReadyNowCount());
        item.put("successionCoverageRate", position.getSuccessionCoverageRate());
        item.put("riskLevel", position.getRiskLevel());
        item.put("riskDescription", position.getRiskDescription());
        return item;
    }

    private List<Map<String, Object>> sanitizeCandidates(List<SuccessionCandidateVO> candidates) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index = 0; index < candidates.size(); index++) {
            SuccessionCandidateVO candidate = candidates.get(index);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("candidate", "候选人" + (index + 1));
            item.put("department", candidate.getDepartment());
            item.put("currentPosition", candidate.getCurrentPosition());
            item.put("workYears", candidate.getWorkYears());
            item.put("matchScore", candidate.getMatchScore());
            item.put("skillScore", candidate.getSkillScore());
            item.put("performanceScore", candidate.getPerformanceScore());
            item.put("potentialScore", candidate.getPotentialScore());
            item.put("experienceScore", candidate.getExperienceScore());
            item.put("matchedSkillCount", candidate.getMatchedSkillCount());
            item.put("requiredSkillCount", candidate.getRequiredSkillCount());
            item.put("missingSkills", candidate.getMissingSkills());
            item.put("readiness", candidate.getReadiness());
            item.put("preparationMonths", candidate.getPreparationMonths());
            item.put("recommendationReason", candidate.getRecommendationReason());
            item.put("dataWarnings", candidate.getDataWarnings());
            result.add(item);
        }
        return result;
    }

    private boolean isConfigured() {
        return StringUtils.hasText(baseUrl) && StringUtils.hasText(apiKey);
    }

    private RestClient restClient() {
        RestClient current = restClient;
        if (current != null) return current;
        synchronized (this) {
            if (restClient == null) {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(10_000);
                factory.setReadTimeout(120_000);
                restClient = RestClient.builder().requestFactory(factory).build();
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

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String shorten(String text) {
        if (!StringUtils.hasText(text)) return "无响应内容";
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 500 ? oneLine : oneLine.substring(0, 500) + "...";
    }
}
