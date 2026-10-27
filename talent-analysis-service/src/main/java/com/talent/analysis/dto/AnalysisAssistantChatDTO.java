package com.talent.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 数据分析智能体的一条用户消息。
 */
@Data
public class AnalysisAssistantChatDTO {

    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "消息不能超过 2000 字")
    private String query;

    private String conversationId;

    private String user;
}
