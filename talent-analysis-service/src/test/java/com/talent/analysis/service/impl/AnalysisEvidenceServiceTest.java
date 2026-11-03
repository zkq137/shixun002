package com.talent.analysis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.analysis.service.AnalysisService;
import com.talent.analysis.service.FlowForecastService;
import com.talent.analysis.service.ModelLogService;
import com.talent.analysis.service.ReportService;
import com.talent.analysis.service.TalentModelService;
import com.talent.analysis.vo.DashboardVO;
import com.talent.analysis.vo.FlowTrendVO;
import com.talent.analysis.vo.PipelineVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisEvidenceServiceTest {

    @Test
    void buildsPipelineContextWithoutLocalHttpCall() {
        AnalysisService analysisService = mock(AnalysisService.class);
        FlowForecastService flowForecastService = mock(FlowForecastService.class);
        TalentModelService talentModelService = mock(TalentModelService.class);
        ModelLogService modelLogService = mock(ModelLogService.class);
        ReportService reportService = mock(ReportService.class);

        PipelineVO pipeline = new PipelineVO();
        pipeline.setHealthScore(82.5);
        pipeline.setHealthLevel("良好");
        when(analysisService.dashboard()).thenReturn(new DashboardVO());
        when(analysisService.pipeline()).thenReturn(pipeline);
        when(analysisService.flowTrend()).thenReturn(new FlowTrendVO());

        AnalysisEvidenceService service = new AnalysisEvidenceService(
                analysisService,
                flowForecastService,
                talentModelService,
                modelLogService,
                reportService,
                new ObjectMapper());
        String context = service.buildContext("分析当前人才梯队健康度");

        assertTrue(context.contains("\"pipeline\""));
        assertTrue(context.contains("\"healthScore\":82.5"));
        assertTrue(context.contains("\"readOnly\":true"));
    }
}