package com.talent.promotion.vo;

import lombok.Data;

/**
 * 晋升智能体返回的一条回复。
 */
@Data
public class AssistantReplyVO {

    private String answer;
    private String conversationId;
    private String messageId;
    private Long createdAt;
}
