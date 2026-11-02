package com.talent.analysis.service;

import com.talent.analysis.vo.FlowForecastVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 人才流动趋势预测、预测结果导入导出。
 */
public interface FlowForecastService {

    FlowForecastVO generate(int horizonMonths);

    FlowForecastVO latest();

    void exportCsv(HttpServletResponse response) throws IOException;

    FlowForecastVO importCsv(MultipartFile file) throws IOException;
}
