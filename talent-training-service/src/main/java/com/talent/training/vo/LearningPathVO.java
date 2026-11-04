package com.talent.training.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LearningPathVO {
    private Long id;
    private String pathName;
    private String targetSkill;
    private String forPosition;
    private String developmentGoal;
    private String matchReason;
    private LocalDateTime createdAt;
    private BigDecimal matchScore;
    private List<PathTaskVO> tasks;

    @Data
    public static class PathTaskVO {
        private Long id;
        private String taskName;
        private Integer stage;
        private Long courseId;
        private String courseName;
    }
}
