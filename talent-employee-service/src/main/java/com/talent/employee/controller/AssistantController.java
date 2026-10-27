package com.talent.employee.controller;

import com.talent.common.result.Result;
import com.talent.employee.dto.AssistantChatDTO;
import com.talent.employee.service.AssistantService;
import com.talent.employee.vo.AssistantReplyVO;
import com.talent.employee.vo.AssistantStatusVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模块一·AI 助手（Dify 工作流代理）。
 *
 * <p>前端只调用本服务，不直接接触 Dify 的 API Key。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @GetMapping("/status")
    public Result<AssistantStatusVO> status() {
        return Result.success(assistantService.status());
    }

    @PostMapping("/chat")
    public Result<AssistantReplyVO> chat(@Valid @RequestBody AssistantChatDTO dto) {
        return Result.success(assistantService.chat(dto));
    }
}
