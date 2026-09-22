package com.talent.planning.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 关键岗位的继任候选人。
 */
@Data
public class SuccessionCandidateVO {

    private Long employeeId;
    private String empNo;
    private String name;
    private String department;
    private String jobRank;
    private String levelTier;
    private BigDecimal tenureYears;
    private BigDecimal perfScore;
    private String potentialLevel;
    private String warningLevel;

    /** 目标岗位要求的核心技能条数 */
    private Integer coreRequire;

    /** 候选人已具备其中的几条 */
    private Integer matched;

    /** 匹配度 = 已具备 / 要求，百分比 */
    private BigDecimal matchScore;
}
