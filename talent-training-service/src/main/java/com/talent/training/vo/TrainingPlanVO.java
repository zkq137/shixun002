package com.talent.training.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TrainingPlanVO {
    private Long employeeId;
    private String employeeName;
    private String positionName;
    private List<CourseItemVO> completedCourses;
    private List<CourseItemVO> recommendedCourses;
    private List<LearningPathVO> recommendedPaths;
    private List<String> skillGaps;
    private String recommendationReason;
    private String algorithmName;
    private java.math.BigDecimal matchThreshold;
    private String developmentGoal;
    private LocalDateTime generatedAt;
}
