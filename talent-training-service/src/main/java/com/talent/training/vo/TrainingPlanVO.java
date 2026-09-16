package com.talent.training.vo;

import lombok.Data;

import java.util.List;

@Data
public class TrainingPlanVO {
    private Long employeeId;
    private String employeeName;
    private String positionName;
    private List<CourseItemVO> completedCourses;
    private List<CourseItemVO> recommendedCourses;
    private List<String> skillGaps;
    private String recommendationReason;
}
