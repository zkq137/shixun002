package com.talent.employee.controller;

import com.talent.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 员工档案 API。示例接口，实际业务请在此模块内扩展。
 */
@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    @GetMapping("/list")
    public Result<List<EmployeeDTO>> list() {
        return Result.success(List.of(
                new EmployeeDTO(1001L, "张三", "研发部", "Java开发", "在职"),
                new EmployeeDTO(1002L, "李四", "产品部", "产品经理", "在职")
        ));
    }

    @GetMapping("/{id}")
    public Result<EmployeeDTO> getById(@PathVariable Long id) {
        return Result.success(new EmployeeDTO(id, "示例员工", "研发部", "后端开发", "在职"));
    }

    public record EmployeeDTO(Long id, String name, String department, String position, String status) {
    }
}
