package com.talent.analysis.controller;

import com.talent.analysis.service.AnalysisService;
import com.talent.analysis.vo.DashboardVO;
import com.talent.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模块五：数据分析与报告。所有数字都是当场从库里聚合出来的。
 *
 * <p>直连：http://127.0.0.1:8085/api/analysis/dashboard
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /** 人才盘点看板：人数、部门/岗位分布、人才标签、潜力、流失风险、绩效分布 */
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(analysisService.dashboard());
    }
}
