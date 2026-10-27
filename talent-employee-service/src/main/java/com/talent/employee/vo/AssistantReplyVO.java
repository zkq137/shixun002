package com.talent.employee.vo;

import lombok.Data;

/**
 * AI 助手返回的一条回复。
 */
@Data
public class AssistantReplyVO {

    private String answer;
    private String conversationId;
    private String messageId;
    private Long createdAt;
}
