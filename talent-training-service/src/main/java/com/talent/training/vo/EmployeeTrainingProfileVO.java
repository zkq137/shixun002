package com.talent.training.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmployeeTrainingProfileVO {
    private Long employeeId;
    private String empNo;
    private String employeeName;
    private String department;
    private String positionName;
    private String levelTier;
    private String jobRank;
    private BigDecimal tenureYears;
    private String status;
    private BigDecimal perfScore;
    private String potentialLevel;
    private String warningLevel;
    private String talentTag;
}
