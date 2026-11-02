package com.talent.analysis.vo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 当前可部署模型摘要。
 */
@Data
public class ModelArtifactVO {

    private Long id;
    private String modelCode;
    private String modelName;
    private String modelType;
    private String version;
    private List<String> features;
    private Map<String, Object> metrics = new LinkedHashMap<>();
    private Integer sampleCount;
    private Integer positiveCount;
    private String datasetVersion;
    private String trainTime;
}
