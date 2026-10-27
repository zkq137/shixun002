package com.talent.employee.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一条年度绩效（含员工基本信息）。
 */
@Data
public class PerformanceVO {

    private Long id;
    private Long employeeId;
    private String empNo;
    private String name;
    private String department;
    private Integer perfYear;
    private BigDecimal score;
    private String grade;
    private LocalDateTime createdAt;

    /** 原始维度得分 JSON，解析后放到 dimensions，不直接返回给前端 */
    @JsonIgnore
    private String dimensionScores;

    /** 解析后的维度得分 */
    private Map<String, BigDecimal> dimensions = new LinkedHashMap<>();
}
