package com.talent.planning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PositionRiskVO {
    private Long id;
    private Long positionId;
    private String positionName;
    private String positionLevel;
    private Boolean key;
    private String riskLevel;
    private String riskDescription;
    private String riskScope;
    private Integer incumbentCount;
    private Integer successorCount;
    private Integer readyNowCount;
    private Integer highRiskEmployeeCount;
    private BigDecimal skillCoverageRate;
    private LocalDateTime checkedAt;
}
