package com.talent.training.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LearningPathVO {
    private Long id;
    private String pathName;
    private String targetSkill;
    private LocalDateTime createdAt;
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
