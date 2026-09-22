package com.talent.analysis.service;

import com.talent.analysis.vo.DashboardVO;
import com.talent.analysis.vo.FlowTrendVO;
import com.talent.analysis.vo.PipelineVO;

/**
 * 模块五：数据分析与报告。
 */
public interface AnalysisService {

    /** 原有看板：人数、分布等基础统计 */
    DashboardVO dashboard();

    /** 梯队整体状态：结构 + 质量 + 风险 + 健康度 */
    PipelineVO pipeline();

    /** 人才流动趋势 */
    FlowTrendVO flowTrend();
}
