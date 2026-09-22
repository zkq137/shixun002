package com.talent.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新增/修改员工时提交的内容。
 */
@Data
public class EmployeeSaveDTO {

    @NotBlank(message = "工号不能为空")
    private String empNo;

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String gender;

    private Integer age;

    private String department;

    @NotNull(message = "岗位不能为空")
    private Long positionId;

    private String levelTier;

    private String jobRank;

    /** 上级工号，可以不填 */
    private String managerEmpNo;

    private String workMode;

    private BigDecimal tenureYears;

    private String education;

    private String phone;

    /** 不传默认在职 */
    private String status;
}
