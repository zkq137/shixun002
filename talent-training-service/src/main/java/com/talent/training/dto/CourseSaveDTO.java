package com.talent.training.dto;

import lombok.Data;

@Data
public class CourseSaveDTO {
    private String courseName;
    private String courseType;
    private String forPosition;
    private String difficulty;
    private Integer duration;
}
