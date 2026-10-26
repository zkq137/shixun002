package com.talent.planning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RiskHandleDTO {
    @NotBlank(message = "处理状态不能为空")
    private String handleStatus;
    @NotBlank(message = "处理人不能为空")
    @Size(max = 64, message = "处理人不能超过64个字符")
    private String handler;
    @Size(max = 500, message = "处理备注不能超过500个字符")
    private String remark;
}
