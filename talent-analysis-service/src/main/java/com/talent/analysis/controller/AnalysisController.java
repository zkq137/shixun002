package com.talent.analysis.controller;

import com.talent.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据分析与报告 API。示例接口，实际业务在此模块内扩展。
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @GetMapping("/dashboard")
    public Result<String> dashboard() {
        return Result.success("梯队健康度仪表盘数据（示例数据）");
    }
}
