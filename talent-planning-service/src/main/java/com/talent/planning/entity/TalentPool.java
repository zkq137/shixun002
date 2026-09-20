package com.talent.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("talent_pool")
public class TalentPool {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String level;
    private Integer totalCapacity;
    private Integer currentCount;
    private Integer gapCount;
    private Integer readyNowCount;
    private Integer readyOneYearCount;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
