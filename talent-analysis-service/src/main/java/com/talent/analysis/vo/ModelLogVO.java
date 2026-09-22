package com.talent.analysis.vo;

import lombok.Data;

/**
 * 模型训练日志（一行一次训练），用于效果分析和日志溯源。
 */
@Data
public class ModelLogVO {

    private Long id;
    private String modelName;
    private String modelType;
    private String version;
    private String trainTime;
    private String status;
    private Integer sampleCount;
    private Integer trainDurationMs;

    /** 评估指标（JSON 字符串，前端解析后展示） */
    private String metrics;

    /** 训练参数（JSON 字符串，溯源用） */
    private String params;

    private String datasetVersion;
    private String remark;
}
