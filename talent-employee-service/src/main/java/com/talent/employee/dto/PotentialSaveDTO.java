package com.talent.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 录入一个员工的潜力指标。
 */
@Data
public class PotentialSaveDTO {

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    /** 指标 key -> 0~100 的得分 */
    private Map<String, BigDecimal> indicators = new LinkedHashMap<>();
}
