package com.talent.promotion.controller;

import com.talent.common.result.Result;
import com.talent.promotion.dto.AssistantChatDTO;
import com.talent.promotion.service.PromotionAssistantService;
import com.talent.promotion.vo.AssistantReplyVO;
import com.talent.promotion.vo.AssistantStatusVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模块四·晋升决策 AI 助手（Dify 工作流代理）。
 */
@RestController
@RequestMapping("/api/promotion/ai")
@RequiredArgsConstructor
public class PromotionAssistantController {

    private final PromotionAssistantService promotionAssistantService;

    @GetMapping("/status")
    public Result<AssistantStatusVO> status() {
        return Result.success(promotionAssistantService.status());
    }

    @PostMapping("/chat")
    public Result<AssistantReplyVO> chat(@Valid @RequestBody AssistantChatDTO dto) {
        return Result.success(promotionAssistantService.chat(dto));
    }
}
