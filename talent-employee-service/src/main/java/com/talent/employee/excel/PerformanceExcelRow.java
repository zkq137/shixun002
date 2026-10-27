package com.talent.employee.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 绩效 Excel 的一行：工号、年份、评分、等级、维度得分(JSON)。
 */
@Data
public class PerformanceExcelRow {

    @ExcelProperty("工号*")
    private String empNo;

    @ExcelProperty("年份*")
    private Integer perfYear;

    @ExcelProperty("绩效评分*")
    private BigDecimal score;

    @ExcelProperty("绩效等级")
    private String grade;

    /** 可选，JSON，例如 {"业绩":4.5,"协作":4.2} */
    @ExcelProperty("维度得分")
    private String dimensionScores;
}
