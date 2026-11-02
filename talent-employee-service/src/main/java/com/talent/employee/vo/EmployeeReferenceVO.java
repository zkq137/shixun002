package com.talent.employee.vo;

import lombok.Data;

/**
 * 从自然语言中解析出的员工引用，只暴露跨模块解析所需字段。
 */
@Data
public class EmployeeReferenceVO {

    private Long id;
    private String empNo;
    private String name;
    private String department;
}
