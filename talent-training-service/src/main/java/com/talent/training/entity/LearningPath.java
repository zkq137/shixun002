package com.talent.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_path")
public class LearningPath {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String pathName;
    private String targetSkill;
    private LocalDateTime createdAt;
}
