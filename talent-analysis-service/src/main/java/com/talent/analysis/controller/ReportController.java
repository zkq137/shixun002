package com.talent.analysis.controller;

import com.talent.analysis.dto.ReportRequest;
import com.talent.analysis.service.ReportService;
import com.talent.analysis.vo.ReportOptionVO;
import com.talent.analysis.vo.ReportResultVO;
import com.talent.analysis.vo.ReportVO;
import com.talent.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 自定义报表：选维度+指标生成报表，可保存、可按模板生成、可回看溯源。
 *
 * <p>直连：http://127.0.0.1:8085/api/analysis/report/options
 * <p>网关：http://127.0.0.1:9090/api/analysis/report/options
 */
@RestController
@RequestMapping("/api/analysis/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 可选的维度和指标 */
    @GetMapping("/options")
    public Result<List<ReportOptionVO>> options() {
        return Result.success(reportService.options());
    }

    /** 生成报表（save=true 时同时存档） */
    @PostMapping("/generate")
    public Result<ReportResultVO> generate(@RequestBody ReportRequest request) {
        return Result.success("报表生成成功", reportService.generate(request));
    }

    /** 按模板一键生成 */
    @PostMapping("/from-template/{templateId}")
    public Result<ReportResultVO> fromTemplate(@PathVariable("templateId") Long templateId) {
        return Result.success("报表生成成功", reportService.generateFromTemplate(templateId));
    }

    /** 历史报表 */
    @GetMapping("/history")
    public Result<List<ReportVO>> history() {
        return Result.success(reportService.history());
    }

    /** 报表模板列表 */
    @GetMapping("/templates")
    public Result<List<ReportVO>> templates() {
        return Result.success(reportService.templates());
    }

    /** 把当前配置存成模板 */
    @PostMapping("/templates")
    public Result<Long> saveTemplate(@RequestBody ReportRequest request) {
        return Result.success("模板保存成功", reportService.saveTemplate(request));
    }

    @DeleteMapping("/templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable("id") Long id) {
        reportService.deleteTemplate(id);
        return Result.success("模板已删除", null);
    }

    /** 报表详情：含明细数据、图表数据、生成时用的模型/数据版本（溯源） */
    @GetMapping("/{id}")
    public Result<ReportVO> detail(@PathVariable("id") Long id) {
        return Result.success(reportService.detail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        reportService.delete(id);
        return Result.success("报表已删除", null);
    }
}
