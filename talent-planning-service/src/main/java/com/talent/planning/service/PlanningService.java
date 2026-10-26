package com.talent.planning.service;

import com.talent.common.result.PageResult;
import com.talent.planning.dto.RiskHandleDTO;
import com.talent.planning.dto.RiskQueryDTO;
import com.talent.planning.vo.*;

import java.util.List;

public interface PlanningService {
    List<PositionVO> positions(String keyword, Boolean key);
    PositionVO positionDetail(Long id);
    void updateKeyPosition(Long id, boolean key);
    List<PositionVO> keyPositions();
    List<SuccessionCandidateVO> succession(Long positionId, int limit);
    List<SuccessionCandidateVO> refreshSuccession(Long positionId);
    List<SkillCoverageVO> skillCoverage(Long positionId);
    List<TalentPoolVO> talentPools();
    TalentPoolVO talentPool(String level);
    List<TalentPoolVO> refreshTalentPools();
    PageResult<RiskEmployeeVO> employeeRisks(RiskQueryDTO query);
    RiskEmployeeVO employeeRisk(Long id);
    void handleEmployeeRisk(Long id, RiskHandleDTO dto);
    List<PositionRiskVO> positionRisks();
    List<PositionRiskVO> evaluatePositionRisks();
    PlanningDashboardVO dashboard();
}
