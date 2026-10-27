package com.talent.employee.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 录入/修改一条年度绩效。
 */
@Data
public class PerformanceSaveDTO {

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    @NotNull(message = "绩效年份不能为空")
    @Min(value = 2000, message = "绩效年份不合法")
    @Max(value = 2100, message = "绩效年份不合法")
    private Integer perfYear;

    @NotNull(message = "绩效评分不能为空")
    @DecimalMin(value = "0", message = "绩效评分不能小于0")
    @DecimalMax(value = "5", message = "绩效评分不能大于5")
    private BigDecimal score;

    private String grade;

    /** 维度得分，可选，如 {"业绩":4.5,"协作":4.2} */
    private Map<String, BigDecimal> dimensions = new LinkedHashMap<>();
}
