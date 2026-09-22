package com.talent.employee.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 员工 Excel 的一行。
 *
 * <p>导入导出共用这一个模型：表头名字就是 Excel 里的列名，带 * 的是导入必填。
 * 导入时只认前 13 列（到技能标签），后面的绩效/潜力/风险列是导出时带出去的。
 */
@Data
@ColumnWidth(16)
public class EmployeeExcelRow {

    @ExcelProperty("工号*")
    private String empNo;

    @ExcelProperty("姓名*")
    private String name;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("年龄")
    private Integer age;

    @ExcelProperty("部门")
    private String department;

    @ExcelProperty("岗位*")
    private String positionName;

    @ExcelProperty("职级层级")
    private String levelTier;

    @ExcelProperty("职级")
    private String jobRank;

    @ExcelProperty("上级工号")
    private String managerEmpNo;

    @ExcelProperty("办公方式")
    private String workMode;

    @ExcelProperty("司龄(年)")
    private BigDecimal tenureYears;

    @ExcelProperty("状态")
    private String status;

    /** 多个技能用顿号分隔，只认技能库里已有的技能 */
    @ExcelProperty("技能标签")
    @ColumnWidth(30)
    private String skills;

    @ExcelProperty("人才标签")
    private String talentTag;

    @ExcelProperty("绩效")
    private BigDecimal perfScore;

    @ExcelProperty("潜力等级")
    private String potentialLevel;

    @ExcelProperty("流失风险")
    private String warningLevel;
}
