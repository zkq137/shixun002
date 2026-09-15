package com.talent.planning.controller;

import com.talent.common.result.Result;
import com.talent.common.vo.NameValueVO;
import com.talent.planning.feign.EmployeeClient;
import com.talent.planning.service.PlanningService;
import com.talent.planning.vo.RiskEmployeeVO;
import com.talent.planning.vo.SuccessionCandidateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 模块二：梯队规划与预测。数据来自 position / pos_skill_require / emp_skill / resign_warning_record。
 *
 * <p>直连：http://127.0.0.1:8082/api/planning/succession?positionId=1&limit=5
 */
@RestController
@RequestMapping("/api/planning")
@RequiredArgsConstructor
public class PlanningController {

    private final PlanningService planningService;
    private final EmployeeClient employeeClient;

    /** 关键岗位的继任候选人：按技能匹配度排序 */
    @GetMapping("/succession")
    public Result<List<SuccessionCandidateVO>> succession(@RequestParam("positionId") Long positionId,
                                                          @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return Result.success(planningService.succession(positionId, limit));
    }

    /** 流失风险最高的员工名单 */
    @GetMapping("/risk")
    public Result<List<RiskEmployeeVO>> risk(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        return Result.success(planningService.topRisk(limit));
    }

    /** 这个岗位要求的核心技能，全公司有多少人具备（找共性缺口） */
    @GetMapping("/skill-coverage")
    public Result<List<NameValueVO>> skillCoverage(@RequestParam("positionId") Long positionId) {
        return Result.success(planningService.skillCoverage(positionId));
    }

    /**
     * 跨服务调用示例：员工档案不存在本模块，通过 Feign 调 talent-employee-service 拿。
     * 员工服务挂了会走 EmployeeClientFallbackFactory 降级，返回 503 和原因。
     */
    @GetMapping("/employee/{id}/profile")
    public Result<Map<String, Object>> employeeProfile(@PathVariable("id") Long id) {
        Result<Map<String, Object>> emp = employeeClient.getEmployee(id);
        if (emp == null || emp.getCode() != 200) {
            return Result.fail(emp == null ? 503 : emp.getCode(),
                    emp == null ? "员工服务暂不可用" : emp.getMessage());
        }
        return emp;
    }
}
