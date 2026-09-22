package com.talent.employee.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除：勾选一批员工。
 */
@Data
public class BatchIdsDTO {

    @NotEmpty(message = "请先勾选员工")
    private List<Long> ids;
}
