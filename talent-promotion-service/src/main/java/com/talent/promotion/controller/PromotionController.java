package com.talent.promotion.controller;

import com.talent.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 晋升决策支持 API。示例接口，实际业务在此模块内扩展。
 */
@RestController
@RequestMapping("/api/promotion")
public class PromotionController {

    @GetMapping("/candidates")
    public Result<String> candidates() {
        return Result.success("符合晋升资质候选人列表（示例数据）");
    }
}
