package com.talent.training.vo;

import lombok.Data;

/** 智能培训推荐助手的 Dify 配置状态。 */
@Data
public class TrainingAssistantStatusVO {
    private boolean configured;
    private String baseUrl;
}
