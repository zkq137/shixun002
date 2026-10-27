package com.talent.employee.service;

import com.talent.employee.dto.PerformanceSaveDTO;
import com.talent.employee.vo.PerformanceCompareVO;
import com.talent.employee.vo.PerformanceImportResultVO;
import com.talent.employee.vo.PerformanceTrendVO;
import com.talent.employee.vo.PerformanceVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 历年绩效：录入、趋势、跨年对比、Excel 导入导出。
 */
public interface PerformanceService {

    List<PerformanceVO> history(Long employeeId);

    PerformanceVO save(PerformanceSaveDTO dto);

    int batchSave(List<PerformanceSaveDTO> records);

    void update(Long id, PerformanceSaveDTO dto);

    void delete(Long id);

    PerformanceTrendVO trend(Long employeeId);

    PerformanceCompareVO compare(Long employeeId, Integer yearA, Integer yearB);

    void export(Long employeeId, HttpServletResponse response) throws IOException;

    void template(HttpServletResponse response) throws IOException;

    PerformanceImportResultVO importExcel(MultipartFile file) throws IOException;
}
