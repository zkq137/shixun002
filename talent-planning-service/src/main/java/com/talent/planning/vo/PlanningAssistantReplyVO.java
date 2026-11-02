package com.talent.planning.vo;

import lombok.Data;

/** 梯队规划智能体回复。 */
@Data
public class PlanningAssistantReplyVO {
    private String answer;
    private String conversationId;
    private String messageId;
    private Long createdAt;
}
