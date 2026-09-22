package com.talent.promotion.service.impl;

import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import com.talent.promotion.mapper.PromotionMapper;
import com.talent.promotion.service.PromotionService;
import com.talent.promotion.vo.PromotionCandidateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模块四业务实现。
 */
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionMapper promotionMapper;

    @Value("${talent.perf-year:2026}")
    private int perfYear;

    @Override
    public List<PromotionCandidateVO> candidates(Long positionId, int limit) {
        if (positionId == null || promotionMapper.countPosition(positionId) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "岗位不存在：" + positionId);
        }
        return promotionMapper.selectCandidates(positionId, perfYear, limit < 1 ? 10 : Math.min(limit, 100));
    }
}
