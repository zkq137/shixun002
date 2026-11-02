package com.talent.analysis.vo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次真实模型训练的结果。
 */
@Data
public class ModelTrainResultVO {

    private String modelCode;
    private String modelName;
    private String modelType;
    private String version;
    private String status;
    private Integer sampleCount;
    private Integer positiveCount;
    private Long trainDurationMs;
    private List<String> features;
    private Map<String, Object> metrics = new LinkedHashMap<>();
    private String message;
}
