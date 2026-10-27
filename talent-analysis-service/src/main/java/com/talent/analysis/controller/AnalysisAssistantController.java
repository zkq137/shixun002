package com.talent.analysis.controller;

import com.talent.analysis.dto.AnalysisAssistantChatDTO;
import com.talent.analysis.service.AnalysisAssistantService;
import com.talent.analysis.vo.AnalysisAssistantReplyVO;
import com.talent.analysis.vo.AnalysisAssistantStatusVO;
import com.talent.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据分析与报告模块的 Dify 智能体代理。
 */
@RestController
@RequestMapping("/api/analysis/ai")
@RequiredArgsConstructor
public class AnalysisAssistantController {

    private final AnalysisAssistantService assistantService;

    @GetMapping("/status")
    public Result<AnalysisAssistantStatusVO> status() {
        return Result.success(assistantService.status());
    }

    @PostMapping("/chat")
    public Result<AnalysisAssistantReplyVO> chat(@Valid @RequestBody AnalysisAssistantChatDTO dto) {
        return Result.success(assistantService.chat(dto));
    }
}
