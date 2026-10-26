package com.talent.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pos_risk")
public class PositionRisk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long positionId;
    private String riskLevel;
    private String riskDesc;
    private String riskScope;
    private Integer incumbentCount;
    private Integer successorCount;
    private Integer readyNowCount;
    private Integer highRiskEmployeeCount;
    private BigDecimal skillCoverageRate;
    private LocalDateTime checkedAt;
}
