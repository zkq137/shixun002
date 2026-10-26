package com.talent.planning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class PlanningDashboardVO {
    private Long positionCount;
    private Long keyPositionCount;
    private Long coveredKeyPositionCount;
    private BigDecimal keyPositionCoverageRate;
    private Long uncoveredKeyPositionCount;
    private Long readyNowCandidateCount;
    private Long totalTalentGap;
    private Long highRiskEmployeeCount;
    private Long unhandledWarningCount;
    private List<PositionRiskVO> highRiskPositions;
    private Map<String, Long> readinessDistribution;
    private List<TalentPoolVO> talentPoolGaps;
}
