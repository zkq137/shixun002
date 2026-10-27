package com.talent.employee.service;

import com.talent.employee.dto.AssistantChatDTO;
import com.talent.employee.vo.AssistantReplyVO;
import com.talent.employee.vo.AssistantStatusVO;

/**
 * 对接 Dify 工作流的 AI 助手。
 */
public interface AssistantService {

    AssistantStatusVO status();

    AssistantReplyVO chat(AssistantChatDTO dto);
}
