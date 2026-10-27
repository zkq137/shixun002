package com.talent.employee.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 两个年度的绩效对比。
 */
@Data
public class PerformanceCompareVO {

    private Long employeeId;
    private String empNo;
    private String name;
    private Integer yearA;
    private Integer yearB;
    private BigDecimal scoreA;
    private BigDecimal scoreB;
    private String gradeA;
    private String gradeB;
    private BigDecimal delta;
    /** 提升 / 下降 / 持平 */
    private String conclusion;
    private List<DimensionDiff> dimensions = new ArrayList<>();

    @Data
    public static class DimensionDiff {
        private String name;
        private BigDecimal valueA;
        private BigDecimal valueB;
        private BigDecimal delta;
    }
}
