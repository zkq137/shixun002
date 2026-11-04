package com.talent.training.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record TrainingSimulationDTO(
        @NotNull @Min(1) @Max(1000000) Integer learners,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal attendRate,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal passRate,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal improveScore) {}
