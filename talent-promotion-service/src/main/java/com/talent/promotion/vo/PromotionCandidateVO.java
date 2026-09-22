package com.talent.promotion.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 晋升候选人：跨层级 + 绩效达标，按综合分排序。
 */
@Data
public class PromotionCandidateVO {

    private Long employeeId;
    private String empNo;
    private String name;
    private String department;
    private String levelTier;
    private String jobRank;
    private BigDecimal tenureYears;
    private BigDecimal perfScore;
    private String potentialLevel;
    private BigDecimal potentialScore;
    private String warningLevel;

    /** 综合分 = 绩效*15 + 潜力评分*0.5 + 司龄*2 */
    private BigDecimal totalScore;
}
