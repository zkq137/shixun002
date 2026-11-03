package com.talent.employee.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.PageResult;
import com.talent.common.result.ResultCode;
import com.talent.employee.dto.EmployeeQuery;
import com.talent.employee.service.EmployeeService;
import com.talent.employee.service.PerformanceService;
import com.talent.employee.service.PotentialService;
import com.talent.employee.service.SkillService;
import com.talent.employee.vo.EmployeeDetailVO;
import com.talent.employee.vo.EmployeeReferenceVO;
import com.talent.employee.vo.EmployeeVO;
import com.talent.employee.vo.PositionOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a bounded, read-only employee context for the Dify chat workflow.
 * Dify never calls the local gateway; it only reasons over this snapshot.
 */
@Service
@RequiredArgsConstructor
public class EmployeeAssistantContextService {

    private static final int MAX_REFERENCED_PROFILES = 5;
    private static final int MAX_EMPLOYEE_ROWS = 50;
    private static final int MAX_POSITION_OPTIONS = 100;
    private static final int MAX_CONTEXT_LENGTH = 48_000;
    private static final Pattern TOP_N_PATTERN = Pattern.compile("(\\d{1,3})\\s*(?:名|位|人|条|个)");

    private final EmployeeService employeeService;
    private final PerformanceService performanceService;
    private final PotentialService potentialService;
    private final SkillService skillService;
    private final ObjectMapper objectMapper;

    public String buildContext(String query) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("module", "employee");
        context.put("readOnly", true);
        context.put("query", query);
        context.put("dataPolicy", List.of(
                "Only use the supplied employee context; never invent records.",
                "Profile data intentionally excludes phone, salary and other unnecessary sensitive fields.",
                "When employeeLookup.matchCount is greater than one, ask for an employee number.",
                "When a requested field is absent, explain the limitation instead of guessing.",
                "This assistant can analyze data but cannot create, update or delete records."
        ));

        List<EmployeeReferenceVO> references = employeeService.referencedEmployees(query);
        Map<String, Object> lookup = new LinkedHashMap<>();
        lookup.put("matchCount", references.size());
        lookup.put("candidates", references);
        context.put("employeeLookup", lookup);

        if (!references.isEmpty()) {
            List<Map<String, Object>> profiles = new ArrayList<>();
            for (EmployeeReferenceVO reference : references.stream().limit(MAX_REFERENCED_PROFILES).toList()) {
                profiles.add(buildProfile(reference.getId()));
            }
            context.put("employeeProfiles", profiles);
        }

        EmployeeQuery employeeQuery = buildEmployeeQuery(query);
        PageResult<EmployeeVO> searchResult = employeeService.pageQuery(employeeQuery);
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("department", employeeQuery.getDepartment());
        search.put("positionId", employeeQuery.getPositionId());
        search.put("talentTag", employeeQuery.getTalentTag());
        search.put("warningLevel", employeeQuery.getWarningLevel());
        search.put("status", employeeQuery.getStatus());
        search.put("total", searchResult.getTotal());
        search.put("employees", searchResult.getRecords());
        context.put("employeeSearch", search);

        Map<String, Object> directory = new LinkedHashMap<>();
        directory.put("totalEmployees", employeeService.count());
        directory.put("departments", employeeService.departmentStats());
        directory.put("positions", employeeService.positionOptions().stream()
                .limit(MAX_POSITION_OPTIONS).toList());
        context.put("directory", directory);

        if (containsAny(query, "技能", "能力", "skill")) {
            context.put("skillCatalog", skillService.tree());
        }

        return serialize(context);
    }

    private EmployeeQuery buildEmployeeQuery(String query) {
        EmployeeQuery employeeQuery = new EmployeeQuery();
        employeeQuery.setPageSize(Math.min(MAX_EMPLOYEE_ROWS, resolveLimit(query, 20)));

        List<String> matchedDepartments = employeeService.departments().stream()
                .filter(StringUtils::hasText)
                .filter(query::contains)
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .toList();
        if (!matchedDepartments.isEmpty()) {
            employeeQuery.setDepartment(matchedDepartments.get(0));
        }

        List<PositionOptionVO> matchedPositions = employeeService.positionOptions().stream()
                .filter(position -> StringUtils.hasText(position.getPositionName()))
                .filter(position -> query.contains(position.getPositionName()))
                .sorted((left, right) -> Integer.compare(
                        right.getPositionName().length(), left.getPositionName().length()))
                .toList();
        if (!matchedPositions.isEmpty()) {
            employeeQuery.setPositionId(matchedPositions.get(0).getId());
        }

        if (query.contains("高风险")) employeeQuery.setWarningLevel("高风险");
        if (query.contains("中风险")) employeeQuery.setWarningLevel("中风险");
        if (query.contains("核心人才")) employeeQuery.setTalentTag("核心人才");
        if (query.contains("储备人才")) employeeQuery.setTalentTag("储备人才");
        if (query.contains("普通员工")) employeeQuery.setTalentTag("普通员工");
        if (query.contains("待优化")) employeeQuery.setTalentTag("待优化");
        if (query.contains("离职")) employeeQuery.setStatus("离职");
        if (query.contains("在职")) employeeQuery.setStatus("在职");
        return employeeQuery;
    }

    private Map<String, Object> buildProfile(Long employeeId) {
        EmployeeDetailVO detail = employeeService.detail(employeeId);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", detail.getId());
        profile.put("empNo", detail.getEmpNo());
        profile.put("name", detail.getName());
        profile.put("department", detail.getDepartment());
        profile.put("positionName", detail.getPositionName());
        profile.put("levelTier", detail.getLevelTier());
        profile.put("jobRank", detail.getJobRank());
        profile.put("tenureYears", detail.getTenureYears());
        profile.put("hireDate", detail.getHireDate());
        profile.put("status", detail.getStatus());
        profile.put("perfScore", detail.getPerfScore());
        profile.put("potentialLevel", detail.getPotentialLevel());
        profile.put("potentialScore", detail.getPotentialScore());
        profile.put("warningLevel", detail.getWarningLevel());
        profile.put("riskScore", detail.getRiskScore());
        profile.put("talentTag", detail.getTalentTag());
        profile.put("skills", detail.getSkills());
        profile.put("courses", detail.getCourses());

        try {
            profile.put("performanceHistory", performanceService.history(employeeId));
            profile.put("performanceTrend", performanceService.trend(employeeId));
        } catch (RuntimeException ignored) {
            profile.put("performanceHistory", List.of());
            profile.put("performanceTrend", null);
        }
        try {
            profile.put("potentialAssessment", potentialService.get(employeeId));
        } catch (RuntimeException ignored) {
            profile.put("potentialAssessment", null);
        }
        return profile;
    }

    private int resolveLimit(String query, int fallback) {
        Matcher matcher = TOP_N_PATTERN.matcher(query);
        if (!matcher.find()) return fallback;
        try {
            return Math.max(1, Math.min(Integer.parseInt(matcher.group(1)), MAX_EMPLOYEE_ROWS));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) return true;
        }
        return false;
    }

    private String serialize(Map<String, Object> context) {
        try {
            String json = objectMapper.writeValueAsString(context);
            if (json.length() <= MAX_CONTEXT_LENGTH) return json;
            return json.substring(0, MAX_CONTEXT_LENGTH)
                    + "\n[context truncated locally because it exceeded the prompt limit]";
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "员工上下文序列化失败");
        }
    }
}
