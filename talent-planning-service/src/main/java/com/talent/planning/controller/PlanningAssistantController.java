package com.talent.planning.controller;

import com.talent.common.result.Result;
import com.talent.planning.dto.PlanningAssistantChatDTO;
import com.talent.planning.service.PlanningAssistantService;
import com.talent.planning.vo.PlanningAssistantReplyVO;
import com.talent.planning.vo.PlanningAssistantStatusVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 梯队规划模块的 Dify 网页版代理。 */
@RestController
@RequestMapping("/api/planning/ai")
@RequiredArgsConstructor
public class PlanningAssistantController {

    private final PlanningAssistantService assistantService;

    @GetMapping("/status")
    public Result<PlanningAssistantStatusVO> status() {
        return Result.success(assistantService.status());
    }

    @PostMapping("/chat")
    public Result<PlanningAssistantReplyVO> chat(
            @Valid @RequestBody PlanningAssistantChatDTO dto) {
        return Result.success(assistantService.chat(dto));
    }
}
