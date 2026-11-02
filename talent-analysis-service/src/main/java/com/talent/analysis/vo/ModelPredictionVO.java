package com.talent.analysis.vo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个员工的模型推理结果。
 */
@Data
public class ModelPredictionVO {

    private String modelCode;
    private String modelName;
    private String modelVersion;
    private Long employeeId;
    private String employeeName;
    private Double score;
    private Double scorePercent;
    private String resultLabel;
    private String explanation;
    private Map<String, Double> topFeatures = new LinkedHashMap<>();
}
