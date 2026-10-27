package com.talent.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    /**
     * 数据集里的列叫 level_tier（职级层级），早期骨架叫 position_level。
     * 这里显式映射到 level_tier，避免 BaseMapper 去查不存在的 position_level 列。
     */
    @TableField("level_tier")
    private String positionLevel;
    private Integer isKey;
    private LocalDateTime createdAt;
}
