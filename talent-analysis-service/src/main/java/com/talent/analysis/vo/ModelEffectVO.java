package com.talent.analysis.vo;

import com.talent.common.vo.NameValueVO;
import lombok.Data;

import java.util.List;

/**
 * 模型运行效果：整体指标 + 分模型汇总 + 版本效果趋势。
 */
@Data
public class ModelEffectVO {

    private Long totalRuns;
    private Long successRuns;
    private Long failedRuns;
    private Double successRate;

    /** 最近一次成功训练的时间 */
    private String lastTrainTime;

    /** 分类任务的平均 auc（失败的不算） */
    private Double avgAuc;

    /** 效果最好的一次 */
    private String bestModel;
    private String bestVersion;
    private Double bestAuc;

    /** 分模型汇总 */
    private List<ModelSummaryVO> modelSummary;

    /** 各版本 auc 趋势（折线图用） */
    private List<NameValueVO> aucTrend;

    /** 训练耗时趋势 */
    private List<NameValueVO> durationTrend;

    @Data
    public static class ModelSummaryVO {
        private String modelName;
        private String modelType;
        private Long runCount;
        private Long successCount;
        private String latestVersion;
        private String latestStatus;
        private String latestTrainTime;
        private Double latestAuc;
        private Double bestAuc;
        private String latestDatasetVersion;
    }
}
