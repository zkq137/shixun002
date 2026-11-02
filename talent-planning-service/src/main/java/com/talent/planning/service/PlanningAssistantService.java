package com.talent.planning.service;

import com.talent.planning.dto.PlanningAssistantChatDTO;
import com.talent.planning.vo.PlanningAssistantReplyVO;
import com.talent.planning.vo.PlanningAssistantStatusVO;

/** 梯队规划 Dify 智能体。 */
public interface PlanningAssistantService {
    PlanningAssistantStatusVO status();
    PlanningAssistantReplyVO chat(PlanningAssistantChatDTO dto);
}
