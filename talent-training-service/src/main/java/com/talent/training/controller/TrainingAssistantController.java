package com.talent.training.controller;

import com.talent.common.result.Result;
import com.talent.training.dto.TrainingAssistantChatDTO;
import com.talent.training.service.TrainingAssistantService;
import com.talent.training.vo.TrainingAssistantReplyVO;
import com.talent.training.vo.TrainingAssistantStatusVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 模块三·智能培训推荐 AI 助手（Dify Chatflow 代理）。 */
@RestController
@RequestMapping("/api/training/ai")
@RequiredArgsConstructor
public class TrainingAssistantController {

    private final TrainingAssistantService trainingAssistantService;

    @GetMapping("/status")
    public Result<TrainingAssistantStatusVO> status() {
        return Result.success(trainingAssistantService.status());
    }

    @PostMapping("/chat")
    public Result<TrainingAssistantReplyVO> chat(@Valid @RequestBody TrainingAssistantChatDTO dto) {
        return Result.success(trainingAssistantService.chat(dto));
    }
}
