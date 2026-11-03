package com.talent.training.vo;

import lombok.Data;

/** 智能培训推荐助手返回的一条回复。 */
@Data
public class TrainingAssistantReplyVO {
    private String answer;
    private String conversationId;
    private String messageId;
    private Long createdAt;
}
