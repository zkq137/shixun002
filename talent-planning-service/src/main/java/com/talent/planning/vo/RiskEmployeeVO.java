package com.talent.planning.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 流失风险名单里的一行。
 */
@Data
public class RiskEmployeeVO {

    private Long employeeId;
    private String empNo;
    private String name;
    private String department;
    private String jobRank;
    private BigDecimal riskScore;
    private String warningLevel;
    private String potentialLevel;
    private String talentTag;
}
