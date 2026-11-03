package com.talent.training.service;

import com.talent.training.dto.TrainingAssistantChatDTO;
import com.talent.training.vo.TrainingAssistantReplyVO;
import com.talent.training.vo.TrainingAssistantStatusVO;

/** 智能培训推荐 Dify 工作流代理。 */
public interface TrainingAssistantService {

    TrainingAssistantStatusVO status();

    TrainingAssistantReplyVO chat(TrainingAssistantChatDTO dto);
}
