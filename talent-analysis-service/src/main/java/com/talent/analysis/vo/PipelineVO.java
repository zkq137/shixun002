package com.talent.analysis.vo;

import com.talent.common.vo.NameValueVO;
import lombok.Data;

import java.util.List;

/**
 * 人才梯队整体状态：结构 + 质量 + 风险 + 健康度评分。
 */
@Data
public class PipelineVO {

    /* ---- 总量 ---- */
    private Long totalEmployees;
    private Long positionCount;
    private Long keyPositionCount;

    /* ---- 人才质量 ---- */
    private Long coreTalentCount;
    private Long reserveTalentCount;
    private Long highPotentialCount;
    private Double avgAge;
    private Double avgTenure;
    private Double avgSalary;
    private Double avgPerf;

    /* ---- 风险 ---- */
    private Long highRiskCount;
    private Long middleRiskCount;
    private Double highRiskRate;

    /* ---- 梯队健康度（0-100，见 AnalysisServiceImpl 里的算法说明）---- */
    private Double healthScore;
    private String healthLevel;
    private List<String> healthSuggestions;

    /* ---- 结构分布 ---- */
    private List<NameValueVO> departmentDistribution;
    private List<NameValueVO> levelTierDistribution;
    private List<NameValueVO> positionTypeDistribution;
    private List<NameValueVO> ageRangeDistribution;
    private List<NameValueVO> tenureRangeDistribution;
    private List<NameValueVO> potentialDistribution;
    private List<NameValueVO> warningDistribution;
    private List<NameValueVO> talentTagDistribution;

    /** 九宫格：绩效(低/中/高) × 潜力(低/中/高) 的人数，3 行 3 列 */
    private List<List<Integer>> nineBox;
}
