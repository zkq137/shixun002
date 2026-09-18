package com.talent.employee.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.talent.common.result.PageResult;
import com.talent.common.result.Result;
import com.talent.common.vo.NameValueVO;
import com.talent.employee.dto.BatchIdsDTO;
import com.talent.employee.dto.EmployeeBatchUpdateDTO;
import com.talent.employee.dto.EmployeeQuery;
import com.talent.employee.dto.EmployeeSaveDTO;
import com.talent.employee.dto.EmployeeSkillDTO;
import com.talent.employee.service.EmployeeExcelService;
import com.talent.employee.service.EmployeeService;
import com.talent.employee.vo.BatchResultVO;
import com.talent.employee.vo.EmployeeDetailVO;
import com.talent.employee.vo.EmployeeImportResultVO;
import com.talent.employee.vo.EmployeeVO;
import com.talent.employee.vo.PositionOptionVO;
import com.talent.employee.vo.SkillVO;
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
 * 模块一：员工档案。数据来自 emp_employee 等表，不再是写死的假数据。
 *
 * <p>直连：http://127.0.0.1:8081/api/employee/page?pageNum=1&pageSize=5
 * <p>网关：http://127.0.0.1:9090/api/employee/page?pageNum=1&pageSize=5
 */
@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeExcelService employeeExcelService;

    /** 快速联调用：默认返回前 10 条 */
    @GetMapping("/list")
    @SentinelResource(value = "employeeList", blockHandler = "listBlockHandler")
    public Result<List<EmployeeVO>> list() {
        return Result.success(employeeService.top(10));
    }

    /**
     * list 的限流兜底方法。约定：方法名对应 blockHandler，
     * 参数 = 原方法参数 + 末尾一个 BlockException，返回值类型与原方法一致。
     */
    public Result<List<EmployeeVO>> listBlockHandler(BlockException ex) {
        return Result.fail(429, "员工列表访问过于频繁，请稍后再试");
    }

    /** 分页 + 条件查询（关键字、部门、岗位、人才标签、风险等级、在职状态） */
    @GetMapping("/page")
    public Result<PageResult<EmployeeVO>> page(EmployeeQuery query) {
        return Result.success(employeeService.pageQuery(query));
    }

    /** 员工详情：基本信息 + 薪酬 + 技能 + 已完成培训 */
    @GetMapping("/{id}")
    public Result<EmployeeDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(employeeService.detail(id));
    }

    /** 新增员工 */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody EmployeeSaveDTO dto) {
        return Result.success("新增成功", employeeService.create(dto));
    }

    /** 修改员工 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody EmployeeSaveDTO dto) {
        employeeService.update(id, dto);
        return Result.success("修改成功", null);
    }

    /** 删除员工（关联的薪酬/绩效/技能/培训记录会一起删掉） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        employeeService.delete(id);
        return Result.success("删除成功", null);
    }

    /** 岗位下拉选项，新增员工时选岗位用 */
    @GetMapping("/options/positions")
    public Result<List<PositionOptionVO>> positionOptions() {
        return Result.success(employeeService.positionOptions());
    }

    /** 部门下拉选项 */
    @GetMapping("/options/departments")
    public Result<List<String>> departments() {
        return Result.success(employeeService.departments());
    }

    /** 按部门统计人数 */
    @GetMapping("/stats/department")
    public Result<List<NameValueVO>> departmentStats() {
        return Result.success(employeeService.departmentStats());
    }

    /** 批量删除 */
    @PostMapping("/batch/delete")
    public Result<BatchResultVO> batchDelete(@Valid @RequestBody BatchIdsDTO dto) {
        return Result.success("删除完成", employeeService.batchDelete(dto.getIds()));
    }

    /** 批量修改部门 / 状态 / 办公方式 / 职级 */
    @PutMapping("/batch/update")
    public Result<BatchResultVO> batchUpdate(@Valid @RequestBody EmployeeBatchUpdateDTO dto) {
        return Result.success("修改完成", employeeService.batchUpdate(dto));
    }

    /** 导出 Excel：导出的是当前筛选条件下的全部数据，不只是当前页 */
    @GetMapping("/export")
    public void export(EmployeeQuery query, HttpServletResponse response) throws IOException {
        employeeExcelService.export(query, response);
    }

    /** 下载导入模板 */
    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        employeeExcelService.template(response);
    }

    /** 导入 Excel */
    @PostMapping("/import")
    public Result<EmployeeImportResultVO> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.success("导入完成", employeeExcelService.importExcel(file));
    }

    /** 某员工会的技能 */
    @GetMapping("/{id}/skills")
    public Result<List<SkillVO>> skills(@PathVariable("id") Long id) {
        return Result.success(employeeService.employeeSkills(id));
    }

    /** 给员工配技能（整体覆盖） */
    @PutMapping("/{id}/skills")
    public Result<Void> updateSkills(@PathVariable("id") Long id, @RequestBody EmployeeSkillDTO dto) {
        employeeService.replaceSkills(id, dto.getSkillIds());
        return Result.success("技能已更新", null);
    }
}
