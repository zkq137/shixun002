package com.talent.planning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PositionVO {
    private Long id;
    private String positionName;
    private String positionLevel;
    private Boolean key;
    private Integer incumbentCount;
    private Integer successorCount;
    private Integer readyNowCount;
    private BigDecimal successionCoverageRate;
    private String riskLevel;
    private String riskDescription;
    private List<SkillCoverageVO> skills;
}
