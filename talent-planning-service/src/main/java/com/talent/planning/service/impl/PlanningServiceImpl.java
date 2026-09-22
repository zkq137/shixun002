package com.talent.planning.service.impl;

import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import com.talent.common.vo.NameValueVO;
import com.talent.planning.mapper.PlanningMapper;
import com.talent.planning.service.PlanningService;
import com.talent.planning.vo.RiskEmployeeVO;
import com.talent.planning.vo.SuccessionCandidateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模块二业务实现。
 */
@Service
@RequiredArgsConstructor
public class PlanningServiceImpl implements PlanningService {

    private final PlanningMapper planningMapper;

    @Value("${talent.perf-year:2026}")
    private int perfYear;

    @Override
    public List<SuccessionCandidateVO> succession(Long positionId, int limit) {
        if (positionId == null || planningMapper.countPosition(positionId) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "岗位不存在：" + positionId);
        }
        return planningMapper.selectSuccessionCandidates(positionId, perfYear, normalize(limit));
    }

    @Override
    public List<RiskEmployeeVO> topRisk(int limit) {
        return planningMapper.selectTopRiskEmployees(normalize(limit));
    }

    @Override
    public List<NameValueVO> skillCoverage(Long positionId) {
        if (positionId == null || planningMapper.countPosition(positionId) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "岗位不存在：" + positionId);
        }
        return planningMapper.selectSkillCoverage(positionId);
    }

    private int normalize(int limit) {
        if (limit < 1) {
            return 10;
        }
        return Math.min(limit, 100);
    }
}
