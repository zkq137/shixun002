package com.talent.training.vo;

import lombok.Data;

/** 问题中提到的岗位，供培训助手提供岗位级学习建议。 */
@Data
public class TrainingPositionReferenceVO {
    private Long id;
    private String positionName;
    private String department;
}
