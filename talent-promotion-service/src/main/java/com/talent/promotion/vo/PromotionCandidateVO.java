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

    /** 人才基础分：绩效*15 + 潜力评分*0.5 + 司龄*2，归一化到 100 */
    private BigDecimal talentScore;

    /** 岗位匹配分：核心技能 80% + 加分技能 20% */
    private BigDecimal skillMatchScore;

    private Integer matchedCoreSkillCount;
    private Integer requiredCoreSkillCount;

    /** 综合分 = 人才基础分 * 60% + 岗位匹配分 * 40% */
    private BigDecimal totalScore;
}
