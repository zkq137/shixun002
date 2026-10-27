package com.talent.employee.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量录入潜力指标。
 */
@Data
public class PotentialBatchDTO {

    @Valid
    @NotEmpty(message = "潜力记录不能为空")
    private List<PotentialSaveDTO> records = new ArrayList<>();
}
