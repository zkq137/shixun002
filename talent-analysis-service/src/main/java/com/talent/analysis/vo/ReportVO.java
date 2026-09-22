package com.talent.analysis.vo;

import lombok.Data;

/**
 * 已保存的报表 / 报表模板。
 */
@Data
public class ReportVO {

    private Long id;
    private String reportName;
    private Long templateId;
    private String dimensions;
    private String metrics;
    private String filters;
    /** 明细数据(JSON) */
    private String reportData;
    /** 图表数据(JSON) */
    private String chartData;
    private Integer rowCount;
    private String modelVersion;
    private String dataVersion;
    private String generatedBy;
    private String createdAt;

    /** 模板专属：模板里的定义（JSON） */
    private String content;
}
