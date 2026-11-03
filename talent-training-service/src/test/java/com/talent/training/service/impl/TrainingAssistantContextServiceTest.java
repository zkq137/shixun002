package com.talent.training.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.common.result.PageResult;
import com.talent.training.mapper.TrainingQueryMapper;
import com.talent.training.service.TrainingService;
import com.talent.training.vo.CourseItemVO;
import com.talent.training.vo.EmployeeTrainingProfileVO;
import com.talent.training.vo.TrainingEmployeeReferenceVO;
import com.talent.training.vo.TrainingPlanVO;
import com.talent.training.vo.TrainingPositionReferenceVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingAssistantContextServiceTest {

    @Test
    void buildsEmployeeAndPositionTrainingEvidence() {
        TrainingService trainingService = mock(TrainingService.class);
        TrainingQueryMapper queryMapper = mock(TrainingQueryMapper.class);

        TrainingEmployeeReferenceVO reference = new TrainingEmployeeReferenceVO();
        reference.setId(1L);
        reference.setEmpNo("E70014679");
        reference.setName("龙秀英");
        reference.setDepartment("行政中心");

        EmployeeTrainingProfileVO profile = new EmployeeTrainingProfileVO();
        profile.setEmployeeId(1L);
        profile.setEmpNo("E70014679");
        profile.setEmployeeName("龙秀英");
        profile.setDepartment("行政中心");
        profile.setPositionName("首席执行官");
        profile.setLevelTier("决策层");
        profile.setPerfScore(new BigDecimal("4.20"));

        CourseItemVO completed = course(1L, "数据分析思维", true, new BigDecimal("96.00"));
        CourseItemVO recommended = course(2L, "领导力提升", false, null);
        TrainingPlanVO plan = new TrainingPlanVO();
        plan.setCompletedCourses(List.of(completed));
        plan.setRecommendedCourses(List.of(recommended));
        plan.setSkillGaps(List.of("战略规划"));
        plan.setRecommendationReason("基于岗位必修课程、已完成培训和岗位技能缺口生成");

        TrainingPositionReferenceVO position = new TrainingPositionReferenceVO();
        position.setId(49L);
        position.setPositionName("市场经理");
        position.setDepartment("行政中心");

        when(queryMapper.employeeReferences("查看龙秀英的培训推荐"))
                .thenReturn(List.of(reference));
        when(queryMapper.employeeProfile(1L)).thenReturn(profile);
        when(queryMapper.positionReferences("查看龙秀英的培训推荐"))
                .thenReturn(List.of(position));
        when(queryMapper.requiredCoursesByPosition(49L)).thenReturn(List.of(recommended));
        when(queryMapper.skillRequirementsByPosition(49L))
                .thenReturn(List.of(Map.of("skillName", "战略规划", "requireType", "核心")));
        when(trainingService.plan(1L)).thenReturn(plan);
        when(trainingService.coursePage(1, 100, null, null))
                .thenReturn(new PageResult<>(1, 1, 100, List.of(recommended)));
        when(trainingService.learningPaths()).thenReturn(List.of());
        when(trainingService.effects()).thenReturn(List.of());

        TrainingAssistantContextService service = new TrainingAssistantContextService(
                trainingService, queryMapper, new ObjectMapper().findAndRegisterModules());
        String context = service.buildContext("查看龙秀英的培训推荐");

        assertThat(context)
                .contains("readOnly")
                .contains("龙秀英")
                .contains("E70014679")
                .contains("领导力提升")
                .contains("战略规划")
                .contains("positionTrainingRequirements")
                .doesNotContain("薪资", "phone");
    }

    private CourseItemVO course(Long id, String name, boolean completed, BigDecimal passRate) {
        CourseItemVO course = new CourseItemVO();
        course.setId(id);
        course.setCourseName(name);
        course.setDifficulty("基础");
        course.setDuration(8);
        course.setCompleted(completed);
        course.setPassRate(passRate);
        return course;
    }
}
