package com.talent.training.vo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.talent.training.dto.TrainingSimulationDTO;
public record TrainingReportVO(LocalDateTime generatedAt, String note, List<Dimension> dimensions, Simulation simulation) {
    public record Dimension(String dimension, String name, long recordCount, long employeeCount,
            BigDecimal attendRate, BigDecimal passRate, BigDecimal improveScore) {}
    public record Simulation(TrainingSimulationDTO assumptions, long estimatedAttendees, long estimatedPassed,
            BigDecimal totalImprovement, BigDecimal overallPassRate) {}
}
