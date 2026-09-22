package com.talent.planning.service;

import com.talent.common.vo.NameValueVO;
import com.talent.planning.vo.RiskEmployeeVO;
import com.talent.planning.vo.SuccessionCandidateVO;

import java.util.List;

/**
 * 模块二：梯队规划与预测。
 */
public interface PlanningService {

    List<SuccessionCandidateVO> succession(Long positionId, int limit);

    List<RiskEmployeeVO> topRisk(int limit);

    List<NameValueVO> skillCoverage(Long positionId);
}
