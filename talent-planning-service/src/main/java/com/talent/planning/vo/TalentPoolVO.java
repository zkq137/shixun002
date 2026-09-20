package com.talent.planning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TalentPoolVO {
    private String level;
    private Integer totalCapacity;
    private Integer currentCount;
    private Integer gapCount;
    private BigDecimal coverageRate;
    private Integer readyNowCount;
    private Integer readyOneYearCount;
    private LocalDateTime updatedAt;
}
