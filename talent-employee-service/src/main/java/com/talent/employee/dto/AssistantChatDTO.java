package com.talent.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 前端发给 AI 助手的一条消息。
 */
@Data
public class AssistantChatDTO {

    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "消息不能超过 2000 字")
    private String query;

    /** Dify 会话ID，首轮为空，后续带上实现多轮上下文 */
    private String conversationId;

    /** 前端用户标识，未传时用服务端默认值 */
    private String user;
}
