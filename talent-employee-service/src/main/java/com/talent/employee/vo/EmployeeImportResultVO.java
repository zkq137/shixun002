package com.talent.employee.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Excel 导入结果：成功多少、失败多少、每行失败原因。
 */
@Data
public class EmployeeImportResultVO {

    private int total;
    private int success;
    private int failed;

    /** 失败明细 */
    private List<RowError> errors = new ArrayList<>();

    /** 文件里出现但技能库里没有的技能名（已跳过，不影响导入成功） */
    private Set<String> unknownSkills = new LinkedHashSet<>();

    public void addError(int row, String empNo, String message) {
        failed++;
        errors.add(new RowError(row, empNo, message));
    }

    @Data
    public static class RowError {
        private final int row;
        private final String empNo;
        private final String message;
    }
}
