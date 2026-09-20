package com.talent.planning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RiskEmployeeVO {
    private Long id;
    private Long employeeId;
    private String empNo;
    private String employeeName;
    private String department;
    private String currentPosition;
    private BigDecimal riskScore;
    private String warningLevel;
    private LocalDateTime warningTime;
    private String handleStatus;
    private String handler;
    private String handleRemark;
    private LocalDateTime handledAt;
}
