package com.talent.employee.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 绩效 Excel 导入结果。
 */
@Data
public class PerformanceImportResultVO {

    private int total;
    private int success;
    private int failed;
    private List<RowError> errors = new ArrayList<>();

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
