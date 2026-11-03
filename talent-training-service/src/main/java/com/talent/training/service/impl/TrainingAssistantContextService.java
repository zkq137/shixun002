package com.talent.training.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.PageResult;
import com.talent.common.result.ResultCode;
import com.talent.training.mapper.TrainingQueryMapper;
import com.talent.training.service.TrainingService;
import com.talent.training.vo.CourseItemVO;
import com.talent.training.vo.EmployeeTrainingProfileVO;
import com.talent.training.vo.LearningPathVO;
import com.talent.training.vo.TrainingEffectVO;
import com.talent.training.vo.TrainingEmployeeReferenceVO;
import com.talent.training.vo.TrainingPlanVO;
import com.talent.training.vo.TrainingPositionReferenceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 在本服务内汇总只读培训证据，再交给 Dify 生成可解释的推荐说明。
 */
@Service
@RequiredArgsConstructor
public class TrainingAssistantContextService {

    private static final Pattern EMPLOYEE_ID_PATTERN = Pattern.compile(
            "(?:员工\\s*ID|员工编号)\\s*[:：]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final int MAX_RELATED_EMPLOYEES = 3;
    private static final int MAX_COURSES = 100;
    private static final int MAX_LEARNING_PATHS = 8;
    private static final int MAX_EFFECTS = 20;
    private static final int MAX_CONTEXT_LENGTH = 48_000;

    private final TrainingService trainingService;
    private final TrainingQueryMapper trainingQueryMapper;
    private final ObjectMapper objectMapper;

    public String buildContext(String query) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("module", "training");
        context.put("readOnly", true);
        context.put("query", query);
        context.put("dataPolicy", List.of(
                "推荐依据是岗位必备课程减去员工已完成培训，remaining recommendedCourses 才是待补课程。",
                "不得编造员工、课程、课程类型、难度、时长、完成记录、培训效果或技能缺口。",
                "employeeLookup.matchCount 大于 1 时先请用户补充工号，不得混合多个员工的数据。",
                "completedCourses 中的课程不得再次推荐；推荐顺序应结合难度、时长和技能缺口说明理由。",
                "课程数据缺失或字段为空时明确说明限制；不得把组织级培训效果当作个人绩效。",
                "这是只读推荐助手，不执行新增、修改、删除、导入或批量操作。"
        ));

        List<TrainingEmployeeReferenceVO> references = resolveReferences(query);
        context.put("employeeLookup", buildLookup(references));

        List<Map<String, Object>> profiles = new ArrayList<>();
        for (TrainingEmployeeReferenceVO reference : references.stream()
                .limit(MAX_RELATED_EMPLOYEES).toList()) {
            EmployeeTrainingProfileVO profile = trainingQueryMapper.employeeProfile(reference.getId());
            if (profile != null) {
                profiles.add(buildEmployeeProfile(profile));
            }
        }
        context.put("trainingProfiles", profiles);

        PageResult<CourseItemVO> coursePage = trainingService
                .coursePage(1, MAX_COURSES, null, null);
        List<CourseItemVO> courses = coursePage.getRecords();
        context.put("courseCatalog", courses.stream().map(this::courseItem).toList());

        List<TrainingPositionReferenceVO> positionReferences =
                trainingQueryMapper.positionReferences(query);
        List<Map<String, Object>> positionDetails = new ArrayList<>();
        for (TrainingPositionReferenceVO position : positionReferences.stream().limit(3).toList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("positionId", position.getId());
            item.put("positionName", position.getPositionName());
            item.put("department", position.getDepartment());
            item.put("requiredCourses", trainingQueryMapper
                    .requiredCoursesByPosition(position.getId()).stream()
                    .map(this::courseItem).toList());
            item.put("skillRequirements", trainingQueryMapper
                    .skillRequirementsByPosition(position.getId()));
            positionDetails.add(item);
        }
        context.put("positionTrainingRequirements", positionDetails);

        List<LearningPathVO> allPaths = trainingService.learningPaths();
        List<LearningPathVO> pathDetails = new ArrayList<>();
        for (LearningPathVO path : allPaths.stream()
                .sorted(Comparator.comparing(LearningPathVO::getId).reversed())
                .limit(MAX_LEARNING_PATHS).toList()) {
            try {
                pathDetails.add(trainingService.learningPathDetail(path.getId()));
            } catch (RuntimeException ignored) {
                pathDetails.add(path);
            }
        }
        context.put("learningPathTemplates", pathDetails);

        List<TrainingEffectVO> effects = trainingService.effects();
        context.put("trainingEffects", effects.stream().limit(MAX_EFFECTS).toList());

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("courseCount", coursePage.getTotal());
        overview.put("learningPathCount", allPaths.size());
        overview.put("trainingEffectCount", effects.size());
        overview.put("includedCourseCount", courses.size());
        overview.put("includedEffectCount", Math.min(effects.size(), MAX_EFFECTS));
        context.put("trainingOverview", overview);

        return serialize(context);
    }

    private List<TrainingEmployeeReferenceVO> resolveReferences(String query) {
        Matcher idMatcher = EMPLOYEE_ID_PATTERN.matcher(query);
        if (idMatcher.find()) {
            try {
                EmployeeTrainingProfileVO profile = trainingQueryMapper
                        .employeeProfile(Long.parseLong(idMatcher.group(1)));
                if (profile != null) {
                    return List.of(toReference(profile));
                }
            } catch (NumberFormatException ignored) {
                // 数字过长时按普通姓名/工号文本继续解析。
            }
        }

        List<TrainingEmployeeReferenceVO> found = trainingQueryMapper.employeeReferences(query);
        Set<Long> seen = new LinkedHashSet<>();
        return found.stream()
                .filter(reference -> reference.getId() != null && seen.add(reference.getId()))
                .toList();
    }

    private Map<String, Object> buildLookup(List<TrainingEmployeeReferenceVO> references) {
        Map<String, Object> lookup = new LinkedHashMap<>();
        lookup.put("matchCount", references.size());
        lookup.put("candidates", references);
        return lookup;
    }

    private TrainingEmployeeReferenceVO toReference(EmployeeTrainingProfileVO profile) {
        TrainingEmployeeReferenceVO reference = new TrainingEmployeeReferenceVO();
        reference.setId(profile.getEmployeeId());
        reference.setEmpNo(profile.getEmpNo());
        reference.setName(profile.getEmployeeName());
        reference.setDepartment(profile.getDepartment());
        return reference;
    }

    private Map<String, Object> buildEmployeeProfile(EmployeeTrainingProfileVO profile) {
        TrainingPlanVO plan = trainingService.plan(profile.getEmployeeId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("employeeId", profile.getEmployeeId());
        item.put("empNo", profile.getEmpNo());
        item.put("employeeName", profile.getEmployeeName());
        item.put("department", profile.getDepartment());
        item.put("positionName", profile.getPositionName());
        item.put("levelTier", profile.getLevelTier());
        item.put("jobRank", profile.getJobRank());
        item.put("tenureYears", profile.getTenureYears());
        item.put("status", profile.getStatus());
        item.put("perfScore", profile.getPerfScore());
        item.put("potentialLevel", profile.getPotentialLevel());
        item.put("warningLevel", profile.getWarningLevel());
        item.put("talentTag", profile.getTalentTag());

        List<CourseItemVO> completed = plan.getCompletedCourses() == null
                ? List.of() : plan.getCompletedCourses();
        List<CourseItemVO> recommended = plan.getRecommendedCourses() == null
                ? List.of() : plan.getRecommendedCourses();

        Map<String, Object> planSection = new LinkedHashMap<>();
        planSection.put("recommendationReason", plan.getRecommendationReason());
        planSection.put("skillGaps", plan.getSkillGaps() == null ? List.of() : plan.getSkillGaps());
        planSection.put("completedCourses", completed.stream().map(this::courseItem).toList());
        planSection.put("recommendedCourses", recommended.stream().map(this::courseItem).toList());
        item.put("trainingPlan", planSection);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("completedCourseCount", completed.size());
        stats.put("completedHours", totalDuration(completed));
        stats.put("recommendedCourseCount", recommended.size());
        stats.put("recommendedHours", totalDuration(recommended));
        stats.put("averageAttendRate", average(completed, CourseItemVO::getAttendRate));
        stats.put("averagePassRate", average(completed, CourseItemVO::getPassRate));
        stats.put("averageImproveScore", average(completed, CourseItemVO::getImproveScore));
        item.put("trainingStats", stats);
        return item;
    }

    private Map<String, Object> courseItem(CourseItemVO course) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("courseId", course.getId());
        item.put("courseName", course.getCourseName());
        item.put("courseType", course.getCourseType());
        item.put("forPosition", course.getForPosition());
        item.put("difficulty", course.getDifficulty());
        item.put("durationHours", course.getDuration());
        item.put("completed", course.getCompleted());
        item.put("attendRate", course.getAttendRate());
        item.put("passRate", course.getPassRate());
        item.put("improveScore", course.getImproveScore());
        item.put("completedAt", course.getCompletedAt());
        return item;
    }

    private int totalDuration(List<CourseItemVO> courses) {
        return courses.stream()
                .map(CourseItemVO::getDuration)
                .filter(duration -> duration != null && duration > 0)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private BigDecimal average(List<CourseItemVO> courses,
                               java.util.function.Function<CourseItemVO, BigDecimal> getter) {
        List<BigDecimal> values = courses.stream()
                .map(getter)
                .filter(value -> value != null)
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private String serialize(Map<String, Object> context) {
        try {
            String json = objectMapper.writeValueAsString(context);
            if (json.length() <= MAX_CONTEXT_LENGTH) {
                return json;
            }
            return json.substring(0, MAX_CONTEXT_LENGTH)
                    + "\n[context truncated locally because it exceeded the prompt limit]";
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "培训上下文序列化失败");
        }
    }
}
