package com.talent.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工年度绩效，对应 emp_performance（一个员工每年一条）。
 */
@Data
@TableName("emp_performance")
public class Performance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    private Integer perfYear;

    private BigDecimal score;

    private String grade;

    /** 各维度得分(JSON)，例如 {"业绩": 4.5, "协作": 4.2} */
    private String dimensionScores;

    private LocalDateTime createdAt;
}
