package com.talent.promotion.vo;

import lombok.Data;

/**
 * 晋升智能体配置状态，前端用来提示是否已配置 Dify。
 */
@Data
public class AssistantStatusVO {

    private boolean configured;
    private String baseUrl;
}
