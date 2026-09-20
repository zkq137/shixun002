package com.talent.planning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SuccessionCandidateVO {
    private Long employeeId;
    private String empNo;
    private String employeeName;
    private String department;
    private String currentPosition;
    private Integer workYears;
    private BigDecimal matchScore;
    private BigDecimal skillScore;
    private BigDecimal performanceScore;
    private BigDecimal potentialScore;
    private BigDecimal experienceScore;
    private Integer matchedSkillCount;
    private Integer requiredSkillCount;
    private List<String> missingSkills;
    private String readiness;
    private Integer preparationMonths;
    private String recommendationReason;
    private List<String> dataWarnings;
}
