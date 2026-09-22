package com.talent.promotion.service;

import com.talent.promotion.vo.PromotionCandidateVO;

import java.util.List;

/**
 * 模块四：晋升决策支持。
 */
public interface PromotionService {

    List<PromotionCandidateVO> candidates(Long positionId, int limit);
}
