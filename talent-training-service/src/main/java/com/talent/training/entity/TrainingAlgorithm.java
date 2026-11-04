package com.talent.training.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@TableName("training_algorithm")
public class TrainingAlgorithm {
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private BigDecimal skillWeight;
    private BigDecimal positionWeight;
    private BigDecimal goalWeight;
    private BigDecimal matchThreshold;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
