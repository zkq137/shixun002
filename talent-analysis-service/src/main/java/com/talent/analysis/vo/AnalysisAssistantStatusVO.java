package com.talent.analysis.vo;

import lombok.Data;

/**
 * 数据分析智能体的 Dify 配置状态。
 */
@Data
public class AnalysisAssistantStatusVO {

    private boolean configured;
    private String baseUrl;
}
