package com.talent.planning.controller;

import com.talent.common.result.Result;
import com.talent.planning.feign.EmployeeClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 梯队规划与预测 API。示例接口，实际业务在此模块内扩展。
 */
@RestController
@RequestMapping("/api/planning")
public class PlanningController {

    private final EmployeeClient employeeClient;

    public PlanningController(EmployeeClient employeeClient) {
        this.employeeClient = employeeClient;
    }

    @GetMapping("/risk")
    public Result<String> riskOverview() {
        // 跨服务获取员工基础数据示例
        Result<java.util.Map<String, Object>> emp = employeeClient.getEmployee(1001L);
        return Result.success("岗位风险计算中 | 已获取员工数据：" + emp.getData());
    }
}
