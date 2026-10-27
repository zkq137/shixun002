package com.talent.employee.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 员工潜力评估结果。
 */
@Data
public class PotentialVO {

    private Long id;
    private Long employeeId;
    private String empNo;
    private String name;
    private String department;
    private String potentialLevel;
    private BigDecimal totalScore;
    private LocalDateTime updatedAt;

    @JsonIgnore
    private String indicatorValues;

    private Map<String, BigDecimal> indicators = new LinkedHashMap<>();
}
