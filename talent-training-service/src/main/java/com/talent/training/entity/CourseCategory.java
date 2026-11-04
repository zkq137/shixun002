package com.talent.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_category")
public class CourseCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String categoryName;
    private String description;
    private LocalDateTime createdAt;
}
