package com.talent.training.dto;

import lombok.Data;

import java.util.List;

@Data
public class LearningPathSaveDTO {
    private String pathName;
    private String targetSkill;
    private List<PathTaskDTO> tasks;

    @Data
    public static class PathTaskDTO {
        private String taskName;
        private Integer stage;
        private Long courseId;
    }
}
