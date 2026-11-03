package com.talent.analysis.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.analysis.service.AnalysisService;
import com.talent.analysis.service.FlowForecastService;
import com.talent.analysis.service.ModelLogService;
import com.talent.analysis.service.ReportService;
import com.talent.analysis.service.TalentModelService;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a bounded, read-only analytics snapshot for a Dify chat workflow.
 * Dify only reasons over the supplied data and never calls the local gateway.
 */
@Service
@RequiredArgsConstructor
public class AnalysisEvidenceService {

    private static final int MAX_CONTEXT_LENGTH = 48_000;
    private static final int MAX_HISTORY_ROWS = 20;

    private final AnalysisService analysisService;
    private final FlowForecastService flowForecastService;
    private final TalentModelService talentModelService;
    private final ModelLogService modelLogService;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    public String buildContext(String query) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("module", "analysis");
        context.put("readOnly", true);
        context.put("query", query);
        context.put("dataPolicy", List.of(
                "Answer only from the supplied analytics snapshot.",
                "Never invent metrics, models, reports or employee facts.",
                "Explain whether a number is a historical observation, a rule score or a forecast.",
                "Forecasts are estimates, not guaranteed outcomes; include confidence and limitations.",
                "This assistant can analyze and explain data but cannot create, update or delete records."
        ));

        context.put("dashboard", analysisService.dashboard());
        context.put("pipeline", analysisService.pipeline());
        context.put("flowTrend", analysisService.flowTrend());

        if (containsAny(query, "预测", "未来", "forecast")) {
            try {
                context.put("latestForecast", flowForecastService.latest());
            } catch (RuntimeException ignored) {
                context.put("latestForecast", null);
            }
        }

        if (containsAny(query, "模型", "训练", "auc", "成功率", "算法", "日志")) {
            try {
                context.put("modelEffect", modelLogService.effect());
                context.put("modelArtifacts", talentModelService.artifacts().stream().limit(20).toList());
                context.put("modelNames", modelLogService.modelNames());
                context.put("recentModelLogs", modelLogService.page(null, null, null, 1, 10).getRecords());
            } catch (RuntimeException ignored) {
                context.put("modelEffect", null);
                context.put("modelArtifacts", List.of());
                context.put("modelNames", List.of());
                context.put("recentModelLogs", List.of());
            }
        }

        if (containsAny(query, "报表", "报告", "维度", "指标", "模板")) {
            try {
                context.put("reportOptions", reportService.options());
                context.put("reportTemplates", reportService.templates().stream()
                        .limit(MAX_HISTORY_ROWS).toList());
                context.put("reportHistory", reportService.history().stream()
                        .limit(MAX_HISTORY_ROWS).toList());
            } catch (RuntimeException ignored) {
                context.put("reportOptions", List.of());
                context.put("reportTemplates", List.of());
                context.put("reportHistory", List.of());
            }
        }

        return serialize(context);
    }

    private boolean containsAny(String text, String... values) {
        String normalized = text == null ? "" : text.toLowerCase();
        for (String value : values) {
            if (normalized.contains(value.toLowerCase())) return true;
        }
        return false;
    }

    private String serialize(Map<String, Object> context) {
        try {
            String json = objectMapper.writeValueAsString(context);
            if (json.length() <= MAX_CONTEXT_LENGTH) return json;
            return json.substring(0, MAX_CONTEXT_LENGTH)
                    + "\n[context truncated locally because it exceeded the prompt limit]";
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.FAIL.getCode(), "分析上下文序列化失败");
        }
    }
}
