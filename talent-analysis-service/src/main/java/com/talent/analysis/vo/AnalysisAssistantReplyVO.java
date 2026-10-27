package com.talent.analysis.vo;

import lombok.Data;

/**
 * 数据分析智能体回复。
 */
@Data
public class AnalysisAssistantReplyVO {

    private String answer;
    private String conversationId;
    private String messageId;
    private Long createdAt;
}
