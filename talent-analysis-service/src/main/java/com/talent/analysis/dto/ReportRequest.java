package com.talent.analysis.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义报表请求：选维度 + 选指标 + 过滤条件。
 */
@Data
public class ReportRequest {

    private String reportName;

    /** 维度 key，如 department、levelTier */
    private List<String> dimensions = new ArrayList<>();

    /** 指标 key，如 count、avgSalary */
    private List<String> metrics = new ArrayList<>();

    /** 过滤条件：字段 key -> 值 */
    private Map<String, String> filters = new LinkedHashMap<>();

    /** 是否把这次生成的结果存进 report 表 */
    private Boolean save = Boolean.FALSE;
}
