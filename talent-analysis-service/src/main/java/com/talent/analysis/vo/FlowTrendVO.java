package com.talent.analysis.vo;

import com.talent.common.vo.NameValueVO;
import lombok.Data;

import java.util.List;

/**
 * 人才流动趋势：流入趋势 + 各批次员工画像 + 流动事件构成。
 */
@Data
public class FlowTrendVO {

    /** 近 24 个月的入职人数（月度） */
    private List<NameValueVO> joinMonthly;

    /** 各年度入职人数 */
    private List<NameValueVO> joinYearly;

    /** 流动事件构成：入职/晋升/调岗/离职 */
    private List<NameValueVO> movementByType;

    /** 司龄段人数分布 */
    private List<NameValueVO> tenureDistribution;

    /** 各司龄段里的高风险人数（看哪个阶段最容易走） */
    private List<NameValueVO> riskByTenure;

    /** 按入职年份看每一批人的现状 */
    private List<CohortVO> cohorts;

    private Long recentJoinCount;
    private Long totalJoin;
    private Long leaveCount;
    private Long promoteCount;
    private Double avgAnnualJoin;

    @Data
    public static class CohortVO {
        /** 入职年份 */
        private String hireYear;
        private Long employeeCount;
        private Double avgTenure;
        private Double avgPerf;
        private Long highRiskCount;
        private Double highRiskRate;
        private Long coreTalentCount;
    }
}
