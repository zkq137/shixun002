package com.talent.training.vo;

import lombok.Data;

/** 从自然语言中解析出的员工引用，只用于培训上下文定位。 */
@Data
public class TrainingEmployeeReferenceVO {
    private Long id;
    private String empNo;
    private String name;
    private String department;
}
