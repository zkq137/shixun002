package com.talent.training.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseItemVO {
    private Long id;
    private String courseName;
    private String courseType;
    private String forPosition;
    private String difficulty;
    private Integer duration;
    private Boolean completed;
    private BigDecimal attendRate;
    private BigDecimal passRate;
    private BigDecimal improveScore;
    private LocalDateTime completedAt;
}
