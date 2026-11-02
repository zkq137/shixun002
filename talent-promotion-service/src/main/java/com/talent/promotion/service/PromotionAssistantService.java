package com.talent.promotion.service;

import com.talent.promotion.dto.AssistantChatDTO;
import com.talent.promotion.vo.AssistantReplyVO;
import com.talent.promotion.vo.AssistantStatusVO;

/**
 * 晋升决策 Dify 工作流代理。
 */
public interface PromotionAssistantService {

    AssistantStatusVO status();

    AssistantReplyVO chat(AssistantChatDTO dto);
}
