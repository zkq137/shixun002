package com.talent.training.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseCategoryVO {
    private Long id;
    private String categoryName;
    private String description;
    private Long courseCount;
    private LocalDateTime createdAt;
}
