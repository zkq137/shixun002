package com.talent.training.dto;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class TrainingAlgorithmSaveDTO {
    private String name;
    private BigDecimal skillWeight;
    private BigDecimal positionWeight;
    private BigDecimal goalWeight;
    private BigDecimal matchThreshold;
    private Boolean enabled;
}
