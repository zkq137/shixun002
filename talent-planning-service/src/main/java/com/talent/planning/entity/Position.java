package com.talent.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("position")
public class Position {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String positionName;
    private String positionLevel;
    private Integer isKey;
    private LocalDateTime createdAt;
}
