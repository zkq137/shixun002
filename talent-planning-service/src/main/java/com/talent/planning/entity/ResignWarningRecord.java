package com.talent.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("resign_warning_record")
public class ResignWarningRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private BigDecimal riskScore;
    private String warningLevel;
    private LocalDateTime warningTime;
    private String handleStatus;
    private String handler;
    private String handleRemark;
    private LocalDateTime handledAt;
}
