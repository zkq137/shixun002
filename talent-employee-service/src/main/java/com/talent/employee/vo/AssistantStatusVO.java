package com.talent.employee.vo;

import lombok.Data;

/**
 * AI 助手配置状态，前端用来提示是否已配置 Dify。
 */
@Data
public class AssistantStatusVO {

    private boolean configured;
    private String baseUrl;
}
