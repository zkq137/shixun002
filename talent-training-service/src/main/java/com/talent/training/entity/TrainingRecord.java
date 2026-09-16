package com.talent.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("training_record")
public class TrainingRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private Long courseId;
    private BigDecimal attendRate;
    private BigDecimal passRate;
    private BigDecimal improveScore;
    private LocalDateTime createdAt;
}
