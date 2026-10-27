package com.talent.employee.controller;

import com.talent.common.result.Result;
import com.talent.employee.dto.PerformanceBatchDTO;
import com.talent.employee.dto.PerformanceSaveDTO;
import com.talent.employee.service.PerformanceService;
import com.talent.employee.vo.PerformanceCompareVO;
import com.talent.employee.vo.PerformanceImportResultVO;
import com.talent.employee.vo.PerformanceTrendVO;
import com.talent.employee.vo.PerformanceVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 模块一·历年绩效。
 *
 * <p>直连：http://127.0.0.1:8081/api/performance/employees/1
 * <p>网关：http://127.0.0.1:9090/api/performance/employees/1
 */
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    /** 某员工的历年绩效 */
    @GetMapping("/employees/{employeeId}")
    public Result<List<PerformanceVO>> history(@PathVariable("employeeId") Long employeeId) {
        return Result.success(performanceService.history(employeeId));
    }

    /** 绩效趋势（自动生成图表数据） */
    @GetMapping("/employees/{employeeId}/trend")
    public Result<PerformanceTrendVO> trend(@PathVariable("employeeId") Long employeeId) {
        return Result.success(performanceService.trend(employeeId));
    }

    /** 跨年份对比 */
    @GetMapping("/employees/{employeeId}/compare")
    public Result<PerformanceCompareVO> compare(@PathVariable("employeeId") Long employeeId,
                                                @RequestParam("yearA") Integer yearA,
                                                @RequestParam("yearB") Integer yearB) {
        return Result.success(performanceService.compare(employeeId, yearA, yearB));
    }

    /** 单条录入（按员工+年份 upsert） */
    @PostMapping
    public Result<PerformanceVO> save(@Valid @RequestBody PerformanceSaveDTO dto) {
        return Result.success("绩效录入成功", performanceService.save(dto));
    }

    /** 批量录入 */
    @PostMapping("/batch")
    public Result<Integer> batchSave(@Valid @RequestBody PerformanceBatchDTO dto) {
        return Result.success("批量录入成功", performanceService.batchSave(dto.getRecords()));
    }

    /** 修改一条 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody PerformanceSaveDTO dto) {
        performanceService.update(id, dto);
        return Result.success("修改成功", null);
    }

    /** 删除一条 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        performanceService.delete(id);
        return Result.success("删除成功", null);
    }

    /** 导出绩效 Excel（可只导一个员工） */
    @GetMapping("/export")
    public void export(@RequestParam(value = "employeeId", required = false) Long employeeId,
                       HttpServletResponse response) throws IOException {
        performanceService.export(employeeId, response);
    }

    /** 绩效导入模板 */
    @GetMapping("/import-template")
    public void template(HttpServletResponse response) throws IOException {
        performanceService.template(response);
    }

    /** 导入绩效 Excel */
    @PostMapping("/import")
    public Result<PerformanceImportResultVO> importExcel(@RequestParam("file") MultipartFile file)
            throws IOException {
        return Result.success("导入完成", performanceService.importExcel(file));
    }
}
