package com.talent.planning.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CandidateSourceVO {
    private Long employeeId;
    private String empNo;
    private String employeeName;
    private String department;
    private String currentPosition;
    private Integer workYears;
    private BigDecimal performanceScore;
    private BigDecimal potentialScore;
    private Integer matchedSkillCount;
    private Integer requiredSkillCount;
}
