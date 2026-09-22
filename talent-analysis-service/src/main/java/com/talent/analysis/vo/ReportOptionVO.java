package com.talent.analysis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 自定义报表可选的维度和指标。
 */
@Data
@AllArgsConstructor
public class ReportOptionVO {

    private String key;
    private String label;
    /** dimension / metric */
    private String type;
}
