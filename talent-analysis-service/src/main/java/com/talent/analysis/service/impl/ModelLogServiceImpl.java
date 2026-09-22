package com.talent.analysis.service.impl;

import com.talent.analysis.mapper.ModelLogMapper;
import com.talent.analysis.service.ModelLogService;
import com.talent.analysis.vo.ModelEffectVO;
import com.talent.analysis.vo.ModelLogVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.PageResult;
import com.talent.common.result.ResultCode;
import com.talent.common.vo.NameValueVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型效果分析实现。
 *
 * <p>效果指标从 model_log.metrics 的 JSON 里取 auc（真实训练产物是啥就存啥），
 * 汇总逻辑写在 Java 里而不是 SQL 里，行数少、可读性好，也方便以后加别的指标。
 */
@Service
@RequiredArgsConstructor
public class ModelLogServiceImpl implements ModelLogService {

    /** 参与汇总的最近日志条数 */
    private static final int RECENT_LIMIT = 200;

    private final ModelLogMapper modelLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public ModelEffectVO effect() {
        List<ModelLogVO> logs = modelLogMapper.selectRecent(RECENT_LIMIT);

        ModelEffectVO vo = new ModelEffectVO();
        long success = logs.stream().filter(log -> "已完成".equals(log.getStatus())).count();
        vo.setTotalRuns((long) logs.size());
        vo.setSuccessRuns(success);
        vo.setFailedRuns(logs.size() - success);
        vo.setSuccessRate(logs.isEmpty() ? 0.0 : round1(success * 100.0 / logs.size()));

        vo.setLastTrainTime(logs.isEmpty() ? null : logs.get(0).getTrainTime());

        // 分类模型看 auc，取所有成功且带 auc 的记录
        List<Double> aucList = new ArrayList<>();
        ModelLogVO best = null;
        for (ModelLogVO log : logs) {
            Double auc = auc(log.getMetrics());
            if (auc == null) {
                continue;
            }
            aucList.add(auc);
            if (best == null || auc > auc(best.getMetrics())) {
                best = log;
            }
        }
        vo.setAvgAuc(aucList.isEmpty() ? null : round4(aucList.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
        if (best != null) {
            vo.setBestModel(best.getModelName());
            vo.setBestVersion(best.getVersion());
            vo.setBestAuc(auc(best.getMetrics()));
        }

        // 分模型汇总：每个模型取最近一次 + 历史最好
        Map<String, ModelEffectVO.ModelSummaryVO> summaryMap = new LinkedHashMap<>();
        for (ModelLogVO log : logs) {
            ModelEffectVO.ModelSummaryVO summary = summaryMap.computeIfAbsent(log.getModelName(), name -> {
                ModelEffectVO.ModelSummaryVO item = new ModelEffectVO.ModelSummaryVO();
                item.setModelName(name);
                item.setModelType(log.getModelType());
                item.setRunCount(0L);
                item.setSuccessCount(0L);
                return item;
            });
            summary.setRunCount(summary.getRunCount() + 1);
            if ("已完成".equals(log.getStatus())) {
                summary.setSuccessCount(summary.getSuccessCount() + 1);
            }
            // logs 已按时间倒序，第一条就是最近的
            if (summary.getLatestVersion() == null) {
                summary.setLatestVersion(log.getVersion());
                summary.setLatestStatus(log.getStatus());
                summary.setLatestTrainTime(log.getTrainTime());
                summary.setLatestAuc(auc(log.getMetrics()));
                summary.setLatestDatasetVersion(log.getDatasetVersion());
            }
            Double auc = auc(log.getMetrics());
            if (auc != null && (summary.getBestAuc() == null || auc > summary.getBestAuc())) {
                summary.setBestAuc(auc);
            }
        }
        vo.setModelSummary(new ArrayList<>(summaryMap.values()));

        // 趋势：按时间正序，方便前端画折线
        List<NameValueVO> aucTrend = new ArrayList<>();
        List<NameValueVO> durationTrend = new ArrayList<>();
        for (int i = logs.size() - 1; i >= 0; i--) {
            ModelLogVO log = logs.get(i);
            Double auc = auc(log.getMetrics());
            if (auc != null) {
                aucTrend.add(new NameValueVO(log.getVersion() + "(" + log.getModelName() + ")", Math.round(auc * 1000)));
            }
            durationTrend.add(new NameValueVO(log.getVersion() + "(" + log.getModelName() + ")",
                    log.getTrainDurationMs() == null ? 0L : log.getTrainDurationMs().longValue() / 1000));
        }
        vo.setAucTrend(aucTrend);
        vo.setDurationTrend(durationTrend);
        return vo;
    }

    @Override
    public PageResult<ModelLogVO> page(String modelName, String status, String version, long pageNum, long pageSize) {
        long current = pageNum < 1 ? 1 : pageNum;
        long size = pageSize < 1 ? 10 : Math.min(pageSize, 100);
        long total = modelLogMapper.countPage(modelName, status, version);
        List<ModelLogVO> records = modelLogMapper.selectPage(modelName, status, version,
                (int) ((current - 1) * size), (int) size);
        return new PageResult<>(total, current, size, records);
    }

    @Override
    public ModelLogVO detail(Long id) {
        ModelLogVO log = id == null ? null : modelLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "训练日志不存在：" + id);
        }
        return log;
    }

    @Override
    public List<String> modelNames() {
        return modelLogMapper.selectModelNames();
    }

    /** 从 metrics JSON 里取 auc，JSON 不合法或没有这个字段就返回 null */
    private Double auc(String metricsJson) {
        if (metricsJson == null || metricsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(metricsJson).path("auc");
            return node.isNumber() ? node.asDouble() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private double round4(double value) {
        return Math.round(value * 10000) / 10000.0;
    }
}
