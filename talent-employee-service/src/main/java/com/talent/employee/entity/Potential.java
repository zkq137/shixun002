package com.talent.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工潜力评估，对应 emp_potential（一个员工一条）。
 */
@Data
@TableName("emp_potential")
public class Potential {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    private String potentialLevel;

    private BigDecimal totalScore;

    /** 各项指标得分(JSON) */
    private String indicatorValues;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
