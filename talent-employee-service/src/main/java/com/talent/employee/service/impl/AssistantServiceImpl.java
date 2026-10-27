package com.talent.employee.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import com.talent.employee.dto.AssistantChatDTO;
import com.talent.employee.entity.Employee;
import com.talent.employee.mapper.EmployeeMapper;
import com.talent.employee.service.AssistantService;
import com.talent.employee.vo.AssistantReplyVO;
import com.talent.employee.vo.AssistantStatusVO;
import com.talent.employee.vo.PositionOptionVO;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dify 工作流代理。
 *
 * <p>API Key 只放在服务端配置里，前端永远拿不到。
 * 兼容两种配置写法：DIFY_BASE_URL 填 Dify 根地址或带 /v1 的 API 地址。
 */
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    private static final Pattern EMP_NO_PATTERN = Pattern.compile(
            "(?:员工工号|工号|员工编号)\\s*[:：]?\\s*([A-Za-z0-9_-]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(?:姓名|名字)\\s*(?:是|为)?\\s*[:：]?\\s*([\\u4e00-\\u9fa5·]{2,10})");

    private final EmployeeMapper employeeMapper;
    private final ObjectMapper objectMapper;

    @Value("${talent.dify.base-url:}")
    private String baseUrl;

    @Value("${talent.dify.api-key:}")
    private String apiKey;

    @Value("${talent.dify.default-user:web-user}")
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
                    "AI 助手尚未配置：请设置环境变量 DIFY_BASE_URL 和 DIFY_API_KEY 后重启员工服务");
        }

        String query = isEmployeeCreationQuery(dto.getQuery())
                ? dto.getQuery()
                : enrichEmployeeReference(dto.getQuery());
        query = enrichPositionReference(query);
        Map<String, Object> body = new LinkedHashMap<>();
        // 当前 Chatflow 把用户需求声明为 query 输入变量，Dify API 会做必填校验。
        body.put("inputs", Map.of("query", query));
        body.put("query", query);
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
            vo.setAnswer(stringValue(response.get("answer")));
            vo.setConversationId(stringValue(response.get("conversation_id")));
            vo.setMessageId(stringValue(response.get("message_id")));
            Object createdAt = response.get("created_at");
            if (createdAt instanceof Number number) {
                vo.setCreatedAt(number.longValue());
            }
            if (!StringUtils.hasText(vo.getAnswer())) {
                throw new BusinessException(ResultCode.FAIL.getCode(),
                        "Dify 没有返回 answer，请确认导入的是 Chatflow（advanced-chat）应用");
            }
            return vo;
        } catch (RestClientResponseException e) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "Dify 调用失败（HTTP " + e.getStatusCode().value() + "）："
                            + shorten(e.getResponseBodyAsString()));
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

    private boolean isEmployeeCreationQuery(String query) {
        return query.contains("员工")
                && (query.contains("新增") || query.contains("添加") || query.contains("录入"));
    }

    /**
     * Dify 当前工作流一次只调用一个接口。绩效、潜力等接口需要员工 ID，
     * 因此在对话入口先把用户写的姓名或工号解析成 ID，再交给工作流执行。
     */
    private String enrichEmployeeReference(String query) {
        List<Employee> references = employeeMapper.selectReferencedEmployees(query);
        if (references.isEmpty()) {
            return query;
        }

        List<Employee> empNoMatches = references.stream()
                .filter(employee -> StringUtils.hasText(employee.getEmpNo())
                        && query.contains(employee.getEmpNo()))
                .toList();
        Employee target;
        if (empNoMatches.size() == 1) {
            target = empNoMatches.get(0);
        } else {
            int maxNameLength = references.stream()
                    .filter(employee -> StringUtils.hasText(employee.getName())
                            && query.contains(employee.getName()))
                    .mapToInt(employee -> employee.getName().length())
                    .max()
                    .orElse(0);
            List<Employee> nameMatches = references.stream()
                    .filter(employee -> StringUtils.hasText(employee.getName())
                            && employee.getName().length() == maxNameLength
                            && query.contains(employee.getName()))
                    .toList();
            if (nameMatches.isEmpty()) {
                return query;
            }
            if (nameMatches.size() > 1) {
                throw new BusinessException(ResultCode.FAIL.getCode(),
                        "姓名不唯一，请补充员工工号后再查询");
            }
            target = nameMatches.get(0);
        }

        String resolved = query;
        if (StringUtils.hasText(target.getEmpNo())) {
            resolved = resolved.replace(target.getEmpNo(), "员工ID " + target.getId());
        }
        if (StringUtils.hasText(target.getName())) {
            resolved = resolved.replace(target.getName(), "员工ID " + target.getId());
        }
        return resolved + "\n[系统解析：员工姓名=" + target.getName()
                + "，员工ID=" + target.getId() + "，工号=" + target.getEmpNo()
                + "。绩效、潜力等接口请直接使用员工ID。]";
    }

    /**
     * 新增员工时允许直接填写岗位名称。Dify 一次只能调用一个接口，
     * 因此由服务端先解析岗位 ID，并明确告诉工作流哪些字段才是必填项。
     */
    private String enrichPositionReference(String query) {
        List<PositionOptionVO> references = employeeMapper.selectReferencedPositions(query);
        if (references.isEmpty()) {
            return query;
        }

        int maxNameLength = references.stream()
                .mapToInt(position -> position.getPositionName().length())
                .max()
                .orElse(0);
        List<PositionOptionVO> matches = references.stream()
                .filter(position -> position.getPositionName().length() == maxNameLength)
                .toList();
        if (matches.size() > 1) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "岗位名称不唯一，请补充部门或直接提供岗位ID");
        }

        PositionOptionVO position = matches.get(0);
        String resolved = query.replace(position.getPositionName(),
                position.getPositionName() + "（岗位ID " + position.getId() + "）");
        if (!isEmployeeCreationQuery(query)) {
            return resolved + "\n[系统解析：岗位名称=" + position.getPositionName()
                    + "，岗位ID=" + position.getId() + "，请使用该岗位ID查询。]";
        }

        Matcher empNoMatcher = EMP_NO_PATTERN.matcher(query);
        Matcher nameMatcher = NAME_PATTERN.matcher(query);
        if (!empNoMatcher.find() || !nameMatcher.find()) {
            return resolved + "\n[系统解析：岗位名称=" + position.getPositionName()
                    + "，岗位ID=" + position.getId()
                    + "。新增员工还需要工号和姓名。]";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("empNo", empNoMatcher.group(1));
        body.put("name", nameMatcher.group(1));
        body.put("positionId", position.getId());
        body.put("department", position.getDepartment());
        body.put("levelTier", position.getLevelTier());
        try {
            String plan = objectMapper.writeValueAsString(Map.of(
                    "method", "POST",
                    "path", "/api/employee",
                    "body", body,
                    "summary", "新增员工：" + nameMatcher.group(1)));
            return resolved + "\n[系统预校验：工号、姓名、岗位ID均已提供，信息完整。"
                    + "禁止返回 NONE，禁止追问性别、年龄、学历、手机号等可选字段。"
                    + "必须原样输出以下调用计划：" + plan + "]";
        } catch (JsonProcessingException e) {
            return resolved;
        }
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

    private String shorten(String text) {
        if (!StringUtils.hasText(text)) {
            return "无响应内容";
        }
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 300 ? oneLine : oneLine.substring(0, 300) + "...";
    }
}
