package com.talent.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pos_succession_candidate")
public class SuccessionCandidate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long positionId;
    private Long employeeId;
    private BigDecimal matchScore;
    private BigDecimal skillScore;
    private BigDecimal performanceScore;
    private BigDecimal potentialScore;
    private BigDecimal experienceScore;
    private String readiness;
    private Integer preparationMonths;
    private String missingSkills;
    private String recommendationReason;
    private String dataWarnings;
    private LocalDateTime calculatedAt;
    private LocalDateTime createdAt;
}
