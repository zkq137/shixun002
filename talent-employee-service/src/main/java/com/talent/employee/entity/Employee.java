package com.talent.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工基本信息，对应表 emp_employee。
 *
 * <p>薪酬、绩效、潜力、流失风险、人才标签都不在这张表里，需要时按 employee_id 去对应的表取，
 * 见 EmployeeMapper 里的几个关联查询。
 */
@Data
@TableName("emp_employee")
public class Employee {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工号 */
    private String empNo;

    private String name;
    private String gender;
    private Integer age;
    private String department;

    /** 岗位ID，关联 position.id */
    private Long positionId;

    private String levelTier;
    private String jobRank;
    private String managerEmpNo;
    private String workMode;
    private BigDecimal tenureYears;

    private LocalDate hireDate;
    private String education;
    private String phone;

    /** 在职/离职/试用 */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
