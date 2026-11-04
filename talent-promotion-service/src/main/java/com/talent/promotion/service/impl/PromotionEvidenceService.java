package com.talent.promotion.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 在本地汇总晋升决策证据，再由后端把上下文推送给 Dify Cloud。
 *
 * <p>Dify 不访问本机接口，因此不需要公网入口或隧道。
 */
@Service
@RequiredArgsConstructor
public class PromotionEvidenceService {

    private static final Pattern POSITION_ID_PATTERN = Pattern.compile(
            "(?:岗位\\s*ID|岗位编号)\\s*[:：]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final int MAX_CONTEXT_LENGTH = 48_000;
    private static final int CANDIDATE_LIMIT = 20;
    private static final int FOCUS_LIMIT = 3;
    private static final int EMPLOYEE_MATCH_LIMIT = 5;

    private final ObjectMapper objectMapper;

    @Value("${talent.local-api.base-url:http://127.0.0.1:9090}")
    private String localApiBaseUrl;

    private volatile RestClient localClient;

    String buildContext(String query) {
        List<Map<String, Object>> positions = fetchList(
                "/api/planning/positions", Map.of(), "岗位列表");
        List<Map<String, Object>> employeeMatches = fetchList(
                "/api/employee/referenced",
                Map.of("text", query),
                "员工姓名解析");
        List<String> warnings = new ArrayList<>();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("generatedAt", LocalDateTime.now().toString());
        context.put("ruleVersion", "promotion-context-v1");
        context.put("privacy",
                "仅传递晋升决策所需字段，不传递手机号、薪酬、年龄、性别等敏感信息");
        context.put("employeeLookup", employeeLookup(query, employeeMatches));
        List<Map<String, Object>> explicitEmployeeProfiles =
                loadEmployeeProfiles(employeeMatches, warnings);
        if (!explicitEmployeeProfiles.isEmpty()) {
            context.put("employeeProfiles", explicitEmployeeProfiles);
        }

        Map<String, Object> position = resolvePosition(query, positions);
        if (position == null) {
            context.put("positionLookup", Map.of(
                    "matched", false,
                    "query", query,
                    "positionCount", positions.size()));
            if (explicitEmployeeProfiles.isEmpty()) {
                context.put("positionDirectory", positions.stream()
                        .limit(80)
                        .map(this::sanitizePositionSummary)
                        .toList());
                context.put("hint", "未识别目标岗位或员工，请让用户补充完整岗位名称、岗位ID、员工姓名或工号");
            } else {
                context.put("hint", "已加载员工档案和历年绩效；未识别目标岗位，"
                        + "只能回答员工档案、绩效趋势等问题，不能给出晋升评审排序");
            }
            context.put("dataWarnings", warnings);
            return serialize(context);
        }

        long positionId = longValue(position.get("id"), -1);
        context.put("positionLookup", Map.of(
                "matched", true,
                "query", query,
                "positionId", positionId));

        Map<String, Object> positionDetail = fetchObjectOrFallback(
                "/api/planning/positions/" + positionId, position);
        context.put("targetPosition", sanitizePosition(positionDetail));

        List<Map<String, Object>> promotionCandidates = fetchList(
                "/api/promotion/candidates",
                Map.of("positionId", String.valueOf(positionId), "limit", String.valueOf(CANDIDATE_LIMIT)),
                "晋升候选池");
        List<Map<String, Object>> successionCandidates = fetchList(
                "/api/planning/succession",
                Map.of("positionId", String.valueOf(positionId), "limit", String.valueOf(CANDIDATE_LIMIT)),
                "继任候选");
        List<Map<String, Object>> skillCoverage = fetchList(
                "/api/planning/skill-coverage",
                Map.of("positionId", String.valueOf(positionId)),
                "岗位技能覆盖");

        List<Map<String, Object>> ranked = rankCandidates(promotionCandidates, successionCandidates);
        int overlapCount = countOverlap(promotionCandidates, successionCandidates);
        List<Map<String, Object>> successionOnly = successionOnlyCandidates(
                promotionCandidates, successionCandidates);

        if (promotionCandidates.isEmpty()) {
            warnings.add("当前岗位没有通过跨层级及绩效硬条件的晋升候选人");
        }
        if (successionCandidates.isEmpty()) {
            warnings.add("当前岗位没有继任候选人数据，无法评估继任准备度");
        }
        if (overlapCount == 0) {
            warnings.add("晋升候选池与继任池无交集，只能给硬条件符合名单，不能直接给出晋升排序");
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("hardCondition", "职级低于目标岗位且 2026 年绩效 >= 4");
        evidence.put("promotionPoolCount", promotionCandidates.size());
        evidence.put("successionPoolCount", successionCandidates.size());
        evidence.put("successionOverlapCount", overlapCount);
        evidence.put("successionOnlyCount", successionOnly.size());
        evidence.put("candidates", ranked);
        evidence.put("successionOnlyCandidates", successionOnly);
        evidence.put("ruleDefinition", ruleDefinition());
        evidence.put("dataWarnings", warnings);
        context.put("promotionEvidence", evidence);
        context.put("skillCoverage", skillCoverage.stream()
                .limit(20)
                .map(this::sanitizeSkillCoverage)
                .toList());
        context.put("focusEmployees", explicitEmployeeProfiles.isEmpty()
                ? loadFocusEmployees(query, rankCandidatesForLookup(
                        promotionCandidates, successionCandidates, ranked), warnings)
                : explicitEmployeeProfiles);
        context.put("dataWarnings", warnings);
        return serialize(context);
    }

    static Map<String, Object> resolvePosition(String query, List<Map<String, Object>> positions) {
        Matcher matcher = POSITION_ID_PATTERN.matcher(query);
        if (matcher.find()) {
            long id = Long.parseLong(matcher.group(1));
            return positions.stream()
                    .filter(position -> longValue(position.get("id"), -1) == id)
                    .findFirst()
                    .orElse(null);
        }
        return positions.stream()
                .filter(position -> StringUtils.hasText(text(position.get("positionName")))
                        && query.contains(text(position.get("positionName"))))
                .max(Comparator.comparingInt(position -> text(position.get("positionName")).length()))
                .orElse(null);
    }

    static List<Map<String, Object>> rankCandidates(List<Map<String, Object>> promotionCandidates,
                                                    List<Map<String, Object>> successionCandidates) {
        Map<Long, Map<String, Object>> successionByEmployee = new LinkedHashMap<>();
        for (Map<String, Object> successor : successionCandidates) {
            successionByEmployee.put(longValue(successor.get("employeeId"), -1), successor);
        }

        List<Map<String, Object>> ranked = new ArrayList<>();
        for (Map<String, Object> promotion : promotionCandidates) {
            long employeeId = longValue(promotion.get("employeeId"), -1);
            Map<String, Object> succession = successionByEmployee.get(employeeId);
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("employeeId", employeeId);
            candidate.put("name", text(promotion.get("name")));
            candidate.put("department", text(promotion.get("department")));
            candidate.put("levelTier", text(promotion.get("levelTier")));
            candidate.put("jobRank", text(promotion.get("jobRank")));
            candidate.put("tenureYears", number(promotion.get("tenureYears"), 0));
            candidate.put("perfScore", number(promotion.get("perfScore"), 0));
            candidate.put("potentialLevel", text(promotion.get("potentialLevel")));
            candidate.put("potentialScore", number(promotion.get("potentialScore"), 0));
            candidate.put("warningLevel", text(promotion.get("warningLevel")));
            candidate.put("talentScore", number(promotion.get("talentScore"), 0));
            candidate.put("skillMatchScore", number(promotion.get("skillMatchScore"), 0));
            candidate.put("matchedCoreSkillCount", number(promotion.get("matchedCoreSkillCount"), 0));
            candidate.put("requiredCoreSkillCount", number(promotion.get("requiredCoreSkillCount"), 0));
            candidate.put("totalScore", number(promotion.get("totalScore"), 0));

            double promotionScore = clamp(number(promotion.get("totalScore"), 0));
            double riskScore = riskScore(text(promotion.get("warningLevel")));
            candidate.put("promotionScore", round(promotionScore));
            candidate.put("riskScore", round(riskScore));

            if (succession == null) {
                candidate.put("evidenceCoverage", "promotion_only");
                candidate.put("successionMatchScore", null);
                candidate.put("skillScore", null);
                candidate.put("readiness", null);
                candidate.put("preparationMonths", null);
                candidate.put("matchedSkillCount", null);
                candidate.put("requiredSkillCount", null);
                candidate.put("missingSkills", List.of());
                candidate.put("decisionScore", round(clamp(0.80 * promotionScore + 0.20 * riskScore)));
                candidate.put("recommendedAction", "待补充继任评估，不直接下晋升结论");
            } else {
                String readiness = text(succession.get("readiness"));
                double successionMatch = number(succession.get("matchScore"), 0);
                double skillScore = number(succession.get("skillScore"), 0);
                int matchedSkillCount = (int) number(succession.get("matchedSkillCount"), 0);
                double preparation = readinessScore(readiness);
                double decisionScore = 0.40 * promotionScore + 0.35 * successionMatch
                        + 0.15 * preparation + 0.10 * riskScore;

                candidate.put("evidenceCoverage", "full");
                candidate.put("successionMatchScore", round(successionMatch));
                candidate.put("skillScore", round(skillScore));
                candidate.put("readiness", readiness);
                candidate.put("preparationMonths", succession.get("preparationMonths"));
                candidate.put("matchedSkillCount", matchedSkillCount);
                candidate.put("requiredSkillCount", succession.get("requiredSkillCount"));
                candidate.put("missingSkills", succession.get("missingSkills") == null
                        ? List.of() : succession.get("missingSkills"));
                candidate.put("decisionScore", round(clamp(decisionScore)));
                candidate.put("recommendedAction", recommendedAction(
                        readiness, skillScore, matchedSkillCount));
            }
            ranked.add(candidate);
        }

        ranked.sort(Comparator
                .comparingInt((Map<String, Object> candidate) ->
                        "full".equals(text(candidate.get("evidenceCoverage"))) ? 0 : 1)
                .thenComparingInt(candidate -> -readinessRank(text(candidate.get("readiness"))))
                .thenComparingDouble(candidate -> -number(candidate.get("decisionScore"), 0)));
        for (int index = 0; index < ranked.size(); index++) {
            ranked.get(index).put("rank", index + 1);
        }
        return ranked;
    }

    private Map<String, Object> employeeLookup(String query,
                                               List<Map<String, Object>> references) {
        Map<String, Object> lookup = new LinkedHashMap<>();
        lookup.put("matched", !references.isEmpty());
        lookup.put("query", query);
        lookup.put("matchCount", references.size());
        lookup.put("matches", references.stream()
                .limit(EMPLOYEE_MATCH_LIMIT)
                .map(this::sanitizeEmployeeReference)
                .toList());
        return lookup;
    }

    private List<Map<String, Object>> loadEmployeeProfiles(
            List<Map<String, Object>> references,
            List<String> warnings) {
        List<Map<String, Object>> profiles = new ArrayList<>();
        for (Map<String, Object> reference : references.stream()
                .limit(EMPLOYEE_MATCH_LIMIT)
                .toList()) {
            long employeeId = longValue(reference.get("id"), -1);
            if (employeeId <= 0) {
                continue;
            }
            try {
                profiles.add(loadEmployeeProfile(employeeId));
            } catch (BusinessException e) {
                String label = firstText(reference.get("name"), String.valueOf(employeeId));
                warnings.add("员工档案、技能或历年绩效读取失败，已跳过：" + label);
            }
        }
        return profiles;
    }

    private Map<String, Object> loadEmployeeProfile(long employeeId) {
        Map<String, Object> employee = fetchObject(
                "/api/employee/" + employeeId, "员工档案");
        Map<String, Object> profile = sanitizeEmployee(employee);
        profile.put("employeeId", employeeId);

        List<Map<String, Object>> skills = fetchList(
                "/api/employee/" + employeeId + "/skills", Map.of(), "员工技能");
        profile.put("skills", skills.stream()
                .limit(30)
                .map(this::sanitizeEmployeeSkill)
                .toList());

        List<Map<String, Object>> history = fetchList(
                "/api/performance/employees/" + employeeId, Map.of(), "历年绩效");
        profile.put("performanceHistory", history.stream()
                .limit(10)
                .map(this::sanitizePerformanceHistory)
                .toList());

        Map<String, Object> trend = fetchObject(
                "/api/performance/employees/" + employeeId + "/trend", "绩效趋势");
        profile.put("performanceTrend", sanitizePerformanceTrend(trend));
        return profile;
    }

    private Map<String, Object> sanitizeEmployeeReference(Map<String, Object> reference) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("employeeId", longValue(reference.get("id"), -1));
        item.put("empNo", text(reference.get("empNo")));
        item.put("name", text(reference.get("name")));
        item.put("department", text(reference.get("department")));
        return item;
    }

    private Map<String, Object> sanitizePerformanceHistory(Map<String, Object> performance) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("perfYear", performance.get("perfYear"));
        item.put("score", performance.get("score"));
        item.put("grade", performance.get("grade"));
        item.put("dimensions", performance.get("dimensions"));
        return item;
    }

    private Map<String, Object> sanitizePerformanceTrend(Map<String, Object> trend) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("employeeId", trend.get("employeeId"));
        item.put("name", trend.get("name"));
        item.put("years", trend.get("years"));
        item.put("scores", trend.get("scores"));
        item.put("grades", trend.get("grades"));
        item.put("dimensionKeys", trend.get("dimensionKeys"));
        item.put("dimensionSeries", trend.get("dimensionSeries"));
        item.put("averageScore", trend.get("averageScore"));
        item.put("maxScore", trend.get("maxScore"));
        item.put("minScore", trend.get("minScore"));
        item.put("yoyChange", trend.get("yoyChange"));
        item.put("trend", trend.get("trend"));
        return item;
    }

    private List<Map<String, Object>> loadFocusEmployees(String query,
                                                          List<Map<String, Object>> candidates,
                                                          List<String> warnings) {
        LinkedHashSet<Long> focusIds = new LinkedHashSet<>();
        for (Map<String, Object> candidate : candidates) {
            String name = firstText(candidate.get("name"), candidate.get("employeeName"));
            String empNo = text(candidate.get("empNo"));
            if ((StringUtils.hasText(name) && query.contains(name))
                    || (StringUtils.hasText(empNo) && query.contains(empNo))) {
                focusIds.add(longValue(candidate.get("employeeId"), -1));
            }
        }

        if (focusIds.isEmpty()) {
            candidates.stream()
                    .filter(candidate -> "full".equals(text(candidate.get("evidenceCoverage"))))
                    .limit(FOCUS_LIMIT)
                    .forEach(candidate -> focusIds.add(longValue(candidate.get("employeeId"), -1)));
        }
        if (focusIds.isEmpty()) {
            candidates.stream()
                    .limit(FOCUS_LIMIT)
                    .forEach(candidate -> focusIds.add(longValue(candidate.get("employeeId"), -1)));
        }

        List<Map<String, Object>> profiles = new ArrayList<>();
        for (Long employeeId : focusIds) {
            if (employeeId == null || employeeId <= 0) {
                continue;
            }
            try {
                profiles.add(loadEmployeeProfile(employeeId));
            } catch (BusinessException e) {
                warnings.add("员工档案、技能或历年绩效读取失败，已跳过该重点候选人的详细证据");
            }
        }
        return profiles;
    }

    private List<Map<String, Object>> rankCandidatesForLookup(
            List<Map<String, Object>> promotionCandidates,
            List<Map<String, Object>> successionCandidates,
            List<Map<String, Object>> ranked) {
        List<Map<String, Object>> lookup = new ArrayList<>();
        for (Map<String, Object> candidate : ranked) {
            Map<String, Object> item = new LinkedHashMap<>(candidate);
            Map<String, Object> raw = promotionCandidates.stream()
                    .filter(promotion -> longValue(promotion.get("employeeId"), -1)
                            == longValue(candidate.get("employeeId"), -2))
                    .findFirst()
                    .orElse(Map.of());
            item.put("empNo", raw.get("empNo"));
            lookup.add(item);
        }
        lookup.addAll(successionCandidates);
        return lookup;
    }

    private List<Map<String, Object>> successionOnlyCandidates(
            List<Map<String, Object>> promotionCandidates,
            List<Map<String, Object>> successionCandidates) {
        Set<Long> promotionIds = new HashSet<>();
        promotionCandidates.forEach(candidate -> promotionIds.add(longValue(candidate.get("employeeId"), -1)));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> successor : successionCandidates) {
            long employeeId = longValue(successor.get("employeeId"), -1);
            if (promotionIds.contains(employeeId)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", text(successor.get("employeeName")));
            item.put("department", text(successor.get("department")));
            item.put("currentPosition", text(successor.get("currentPosition")));
            item.put("matchScore", number(successor.get("matchScore"), 0));
            item.put("readiness", text(successor.get("readiness")));
            item.put("missingSkills", successor.get("missingSkills"));
            item.put("note", "未满足晋升硬条件，不进入晋升评审");
            result.add(item);
            if (result.size() >= 5) {
                break;
            }
        }
        return result;
    }

    private int countOverlap(List<Map<String, Object>> promotionCandidates,
                             List<Map<String, Object>> successionCandidates) {
        Set<Long> promotionIds = new HashSet<>();
        promotionCandidates.forEach(candidate -> promotionIds.add(longValue(candidate.get("employeeId"), -1)));
        int count = 0;
        for (Map<String, Object> successor : successionCandidates) {
            if (promotionIds.contains(longValue(successor.get("employeeId"), -1))) {
                count++;
            }
        }
        return count;
    }

    private Map<String, Object> sanitizePositionSummary(Map<String, Object> position) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("positionId", longValue(position.get("id"), -1));
        item.put("positionName", text(position.get("positionName")));
        item.put("positionLevel", text(position.get("positionLevel")));
        item.put("key", position.get("key"));
        return item;
    }

    private Map<String, Object> sanitizePosition(Map<String, Object> position) {
        Map<String, Object> item = sanitizePositionSummary(position);
        item.put("department", position.get("department"));
        item.put("incumbentCount", position.get("incumbentCount"));
        item.put("successorCount", position.get("successorCount"));
        item.put("readyNowCount", position.get("readyNowCount"));
        item.put("successionCoverageRate", position.get("successionCoverageRate"));
        item.put("riskLevel", position.get("riskLevel"));
        item.put("riskDescription", position.get("riskDescription"));
        return item;
    }

    private Map<String, Object> sanitizeSkillCoverage(Map<String, Object> skill) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("skillName", skill.get("skillName"));
        item.put("requirementType", skill.get("requirementType"));
        item.put("requiredLevel", skill.get("requiredLevel"));
        item.put("requiredCount", skill.get("requiredCount"));
        item.put("qualifiedCount", skill.get("qualifiedCount"));
        item.put("gapCount", skill.get("gapCount"));
        item.put("coverageRate", skill.get("coverageRate"));
        return item;
    }

    private Map<String, Object> sanitizeEmployee(Map<String, Object> employee) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("name", employee.get("name"));
        profile.put("department", employee.get("department"));
        profile.put("positionName", employee.get("positionName"));
        profile.put("levelTier", employee.get("levelTier"));
        profile.put("jobRank", employee.get("jobRank"));
        profile.put("tenureYears", employee.get("tenureYears"));
        profile.put("status", employee.get("status"));
        profile.put("perfScore", employee.get("perfScore"));
        profile.put("potentialLevel", employee.get("potentialLevel"));
        profile.put("potentialScore", employee.get("potentialScore"));
        profile.put("warningLevel", employee.get("warningLevel"));
        profile.put("riskScore", employee.get("riskScore"));
        profile.put("talentTag", employee.get("talentTag"));
        profile.put("courses", employee.get("courses"));
        return profile;
    }

    private Map<String, Object> sanitizeEmployeeSkill(Map<String, Object> skill) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("skillName", skill.get("skillName"));
        item.put("skillCategory", skill.get("skillCategory"));
        item.put("level", skill.get("level"));
        return item;
    }

    private Map<String, Object> ruleDefinition() {
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("promotionScore", "clamp(totalScore, 0, 100)");
        rules.put("fullEvidenceDecisionScore",
                "0.40 * promotionScore + 0.35 * successionMatchScore + 0.15 * readinessScore + 0.10 * riskScore");
        rules.put("promotionOnlyDecisionScore", "0.80 * promotionScore + 0.20 * riskScore");
        rules.put("readinessScore", Map.of(
                "READY_NOW", 100,
                "READY_1_YEAR", 85,
                "READY_2_YEARS", 55,
                "NOT_READY", 20));
        rules.put("riskScore", Map.of("低风险", 100, "中风险", 60, "高风险", 30));
        rules.put("sorting",
                "full 证据优先；同组按 READY_NOW > READY_1_YEAR > READY_2_YEARS > NOT_READY，再按 decisionScore 降序");
        rules.put("guardrail",
                "READY_2_YEARS、NOT_READY、核心技能匹配为 0 或技能分低于 40 的候选人不得直接推荐晋升");
        return rules;
    }

    private List<Map<String, Object>> fetchList(String path,
                                                Map<String, String> params,
                                                String resourceName) {
        Object data = fetchData(path, params, resourceName);
        if (!(data instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> typed = new LinkedHashMap<>();
                map.forEach((key, value) -> typed.put(String.valueOf(key), value));
                result.add(typed);
            }
        }
        return result;
    }

    private Map<String, Object> fetchObject(String path, String resourceName) {
        Object data = fetchData(path, Map.of(), resourceName);
        if (data instanceof Map<?, ?> map) {
            Map<String, Object> typed = new LinkedHashMap<>();
            map.forEach((key, value) -> typed.put(String.valueOf(key), value));
            return typed;
        }
        throw new BusinessException(ResultCode.FAIL.getCode(), resourceName + "返回格式不正确");
    }

    private Map<String, Object> fetchObjectOrFallback(String path, Map<String, Object> fallback) {
        try {
            return fetchObject(path, "岗位详情");
        } catch (BusinessException e) {
            return fallback;
        }
    }

    private Object fetchData(String path, Map<String, String> params, String resourceName) {
        try {
            Map<String, Object> response = localClient()
                    .get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);
                        params.forEach(uriBuilder::queryParam);
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() { });
            if (response == null) {
                throw new BusinessException(ResultCode.FAIL.getCode(), resourceName + "返回为空");
            }
            int code = (int) number(response.get("code"), 500);
            if (code != 200) {
                throw new BusinessException(code,
                        "读取" + resourceName + "失败：" + text(response.get("message")));
            }
            return response.get("data");
        } catch (RestClientResponseException e) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "读取" + resourceName + "失败（HTTP " + e.getStatusCode().value() + "）");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL.getCode(),
                    "读取" + resourceName + "失败：" + e.getMessage());
        }
    }

    private RestClient localClient() {
        RestClient current = localClient;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (localClient == null) {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(5_000);
                factory.setReadTimeout(30_000);
                String baseUrl = StringUtils.hasText(localApiBaseUrl)
                        ? localApiBaseUrl.trim().replaceAll("/+$", "")
                        : "http://127.0.0.1:9090";
                localClient = RestClient.builder()
                        .baseUrl(baseUrl)
                        .requestFactory(factory)
                        .build();
            }
            return localClient;
        }
    }

    private String serialize(Map<String, Object> context) {
        try {
            String json = objectMapper.writeValueAsString(context);
            return json.length() <= MAX_CONTEXT_LENGTH
                    ? json
                    : json.substring(0, MAX_CONTEXT_LENGTH) + "\n[上下文过长，已在本地截断]";
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "晋升上下文序列化失败");
        }
    }

    private static String recommendedAction(String readiness, double skillScore, int matchedSkillCount) {
        int readinessRank = readinessRank(readiness);
        if (readinessRank <= 0 || matchedSkillCount <= 0 || skillScore < 40) {
            return "进入继任池，暂不晋升";
        }
        if (skillScore >= 70) {
            return "优先进入晋升评审";
        }
        if (skillScore >= 50) {
            return "补齐关键技能后进入晋升评审";
        }
        return "进入继任池，完成技能补强后再评估";
    }

    private static int readinessRank(String readiness) {
        return switch (readiness) {
            case "READY_NOW" -> 4;
            case "READY_1_YEAR" -> 3;
            case "READY_2_YEARS" -> 2;
            case "NOT_READY" -> 1;
            default -> 0;
        };
    }

    private static double readinessScore(String readiness) {
        return switch (readiness) {
            case "READY_NOW" -> 100;
            case "READY_1_YEAR" -> 85;
            case "READY_2_YEARS" -> 55;
            case "NOT_READY" -> 20;
            default -> 0;
        };
    }

    private static double riskScore(String warningLevel) {
        if (warningLevel.contains("高")) return 30;
        if (warningLevel.contains("中")) return 60;
        if (warningLevel.contains("低")) return 100;
        return 60;
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static long longValue(Object value, long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? defaultValue : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double number(Object value, double defaultValue) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? defaultValue : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String firstText(Object... values) {
        for (Object value : values) {
            String text = text(value);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return "";
    }
}
