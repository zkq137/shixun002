package com.talent.analysis.controller;

import com.talent.analysis.service.FlowForecastService;
import com.talent.analysis.vo.FlowForecastVO;
import com.talent.common.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 人才流入、流出、晋升趋势预测。
 */
@RestController
@RequestMapping("/api/analysis/forecast")
@RequiredArgsConstructor
public class FlowForecastController {

    private final FlowForecastService flowForecastService;

    @PostMapping("/generate")
    public Result<FlowForecastVO> generate(
            @RequestParam(value = "horizonMonths", defaultValue = "6") int horizonMonths) {
        return Result.success("预测生成成功", flowForecastService.generate(horizonMonths));
    }

    @GetMapping("/latest")
    public Result<FlowForecastVO> latest() {
        return Result.success(flowForecastService.latest());
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        flowForecastService.exportCsv(response);
    }

    @PostMapping("/import")
    public Result<FlowForecastVO> importCsv(@RequestParam("file") MultipartFile file)
            throws IOException {
        return Result.success("预测数据导入成功", flowForecastService.importCsv(file));
    }
}
