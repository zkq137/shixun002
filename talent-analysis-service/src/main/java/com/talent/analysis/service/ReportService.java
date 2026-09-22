package com.talent.analysis.service;

import com.talent.analysis.dto.ReportRequest;
import com.talent.analysis.vo.ReportOptionVO;
import com.talent.analysis.vo.ReportResultVO;
import com.talent.analysis.vo.ReportVO;

import java.util.List;

/**
 * 自定义报表：选维度 + 选指标 + 过滤条件 → 生成 → 可保存、可回看、可溯源。
 */
public interface ReportService {

    /** 可供选择的维度和指标 */
    List<ReportOptionVO> options();

    /** 生成报表 */
    ReportResultVO generate(ReportRequest request);

    /** 按模板生成（模板里预置了维度和指标） */
    ReportResultVO generateFromTemplate(Long templateId);

    /** 历史报表 */
    List<ReportVO> history();

    /** 报表详情（含明细数据，用于溯源） */
    ReportVO detail(Long id);

    void delete(Long id);

    /** 报表模板列表 */
    List<ReportVO> templates();

    /** 把当前这套配置存成模板 */
    Long saveTemplate(ReportRequest request);

    void deleteTemplate(Long id);
}
