package com.talent.employee.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量修改：勾选一批员工，统一改某几个字段。
 * 只填想改的字段，没填的字段不动。
 */
@Data
public class EmployeeBatchUpdateDTO {

    @NotEmpty(message = "请先勾选员工")
    private List<Long> ids;

    private String department;

    private String status;

    private String workMode;

    private String levelTier;

    private String jobRank;

    /** 邮箱之类的扩展字段以后加在这里 */
}
