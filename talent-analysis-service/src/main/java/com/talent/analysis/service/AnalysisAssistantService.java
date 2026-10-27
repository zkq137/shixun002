package com.talent.analysis.service;

import com.talent.analysis.dto.AnalysisAssistantChatDTO;
import com.talent.analysis.vo.AnalysisAssistantReplyVO;
import com.talent.analysis.vo.AnalysisAssistantStatusVO;

/**
 * 数据分析 Dify 智能体。
 */
public interface AnalysisAssistantService {

    AnalysisAssistantStatusVO status();

    AnalysisAssistantReplyVO chat(AnalysisAssistantChatDTO dto);
}
