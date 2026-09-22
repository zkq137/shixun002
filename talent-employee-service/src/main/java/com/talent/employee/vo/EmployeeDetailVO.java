package com.talent.employee.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 员工详情：列表字段 + 薪酬 + 上级姓名 + 技能标签 + 已完成培训。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeDetailVO extends EmployeeVO {

    private BigDecimal baseSalary;
    private BigDecimal salaryCoefficient;
    private BigDecimal potentialScore;
    private BigDecimal riskScore;
    private String managerName;
    private LocalDate hireDate;
    private String education;
    private String phone;

    /** 会哪些技能，来自 emp_skill + skill */
    private List<String> skills;

    /** 已完成哪些培训，来自 training_record + course */
    private List<String> courses;
}
