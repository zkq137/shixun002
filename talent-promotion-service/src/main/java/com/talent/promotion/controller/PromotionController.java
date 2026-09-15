package com.talent.promotion.controller;

import com.talent.common.result.Result;
import com.talent.promotion.service.PromotionService;
import com.talent.promotion.vo.PromotionCandidateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模块四：晋升决策支持。
 *
 * <p>直连：http://127.0.0.1:8084/api/promotion/candidates?positionId=1&limit=5
 */
@RestController
@RequestMapping("/api/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    /** 目标岗位的晋升候选人，按综合分排序 */
    @GetMapping("/candidates")
    public Result<List<PromotionCandidateVO>> candidates(@RequestParam("positionId") Long positionId,
                                                         @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return Result.success(promotionService.candidates(positionId, limit));
    }
}
