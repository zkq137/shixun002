package com.talent.planning.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkillCoverageVO {
    private Long skillId;
    private String skillName;
    private String requirementType;
    private String requiredLevel;
    private Integer requiredCount;
    private Integer qualifiedCount;
    private Integer gapCount;
    private BigDecimal coverageRate;
}
