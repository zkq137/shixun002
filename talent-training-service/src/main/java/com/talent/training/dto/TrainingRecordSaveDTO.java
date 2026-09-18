package com.talent.training.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TrainingRecordSaveDTO {
    private Long employeeId;
    private Long courseId;
    private BigDecimal attendRate;
    private BigDecimal passRate;
    private BigDecimal improveScore;
}
