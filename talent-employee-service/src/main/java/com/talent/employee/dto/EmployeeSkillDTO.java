package com.talent.employee.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 给员工配技能（整体覆盖）。
 */
@Data
public class EmployeeSkillDTO {

    private List<Long> skillIds = new ArrayList<>();
}
