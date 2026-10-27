package com.talent.employee.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 员工历年绩效趋势。
 */
@Data
public class PerformanceTrendVO {

    private Long employeeId;
    private String empNo;
    private String name;
    private List<Integer> years = new ArrayList<>();
    private List<BigDecimal> scores = new ArrayList<>();
    private List<String> grades = new ArrayList<>();
    private List<String> dimensionKeys = new ArrayList<>();
    /** 每个维度的逐年得分 */
    private List<Series> dimensionSeries = new ArrayList<>();
    private BigDecimal averageScore;
    private BigDecimal maxScore;
    private BigDecimal minScore;
    /** 最近两年差值 */
    private BigDecimal yoyChange;
    /** 上升 / 下降 / 平稳 */
    private String trend;

    @Data
    public static class Series {
        private String name;
        private List<BigDecimal> data = new ArrayList<>();
    }
}
