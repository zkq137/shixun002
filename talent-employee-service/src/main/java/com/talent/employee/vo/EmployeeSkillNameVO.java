package com.talent.employee.vo;

import lombok.Data;

/**
 * 员工 + 技能名，导出时用来拼「技能标签」列。
 */
@Data
public class EmployeeSkillNameVO {

    private Long employeeId;
    private String skillName;
}
