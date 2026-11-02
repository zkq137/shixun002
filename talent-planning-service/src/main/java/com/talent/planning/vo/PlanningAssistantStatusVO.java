package com.talent.planning.vo;

import lombok.Data;

/** 梯队规划智能体的 Dify 配置状态。 */
@Data
public class PlanningAssistantStatusVO {
    private boolean configured;
    private String baseUrl;
}
