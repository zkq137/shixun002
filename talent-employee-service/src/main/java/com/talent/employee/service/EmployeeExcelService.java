package com.talent.employee.service;

import com.talent.employee.dto.EmployeeQuery;
import com.talent.employee.vo.EmployeeImportResultVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 员工档案的 Excel 导入导出。
 */
public interface EmployeeExcelService {

    /** 按当前筛选条件导出（和页面上看到的一致） */
    void export(EmployeeQuery query, HttpServletResponse response) throws IOException;

    /** 下载导入模板 */
    void template(HttpServletResponse response) throws IOException;

    /** 导入，返回成功/失败明细 */
    EmployeeImportResultVO importExcel(MultipartFile file) throws IOException;
}
