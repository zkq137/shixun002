package com.talent.employee.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量录入年度绩效。
 */
@Data
public class PerformanceBatchDTO {

    @Valid
    @NotEmpty(message = "绩效记录不能为空")
    private List<PerformanceSaveDTO> records = new ArrayList<>();
}
