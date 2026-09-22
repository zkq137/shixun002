package com.talent.employee.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 员工列表里的一行（基本信息 + 岗位名称 + 绩效/潜力/风险/标签）。
 */
@Data
public class EmployeeVO {

    private Long id;
    private String empNo;
    private String name;
    private String gender;
    private Integer age;
    private String department;
    private Long positionId;
    private String positionName;
    private String levelTier;
    private String jobRank;
    private String managerEmpNo;
    private String workMode;
    private BigDecimal tenureYears;
    private String status;

    private BigDecimal perfScore;
    private String potentialLevel;
    private String warningLevel;
    private String talentTag;
}
