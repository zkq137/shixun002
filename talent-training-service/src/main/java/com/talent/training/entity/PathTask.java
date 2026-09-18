package com.talent.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("path_task")
public class PathTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pathId;
    private String taskName;
    private Integer stage;
    private Long courseId;
}
