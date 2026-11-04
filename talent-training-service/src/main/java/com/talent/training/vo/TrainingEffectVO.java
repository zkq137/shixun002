package com.talent.training.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TrainingEffectVO {
    private Long courseId;
    private String courseName;
    private Long participantCount;
    private Long attendSampleCount;
    private Long passSampleCount;
    private Long improveSampleCount;
    private BigDecimal averageAttendRate;
    private BigDecimal averagePassRate;
    private BigDecimal averageImproveScore;
}
