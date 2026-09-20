package com.talent.planning.controller;

import com.talent.common.result.PageResult;
import com.talent.common.result.Result;
import com.talent.planning.dto.KeyPositionUpdateDTO;
import com.talent.planning.dto.RiskHandleDTO;
import com.talent.planning.dto.RiskQueryDTO;
import com.talent.planning.feign.EmployeeClient;
import com.talent.planning.service.PlanningService;
import com.talent.planning.vo.PlanningDashboardVO;
import com.talent.planning.vo.PositionRiskVO;
import com.talent.planning.vo.PositionVO;
import com.talent.planning.vo.RiskEmployeeVO;
import com.talent.planning.vo.SkillCoverageVO;
import com.talent.planning.vo.SuccessionCandidateVO;
import com.talent.planning.vo.TalentPoolVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 模块二：梯队规划与预测。 */
@RestController
@RequestMapping("/api/planning")
@RequiredArgsConstructor
public class PlanningController {
    private final PlanningService planningService;
    private final EmployeeClient employeeClient;

    @GetMapping("/positions")
    public Result<List<PositionVO>> positions(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "key", required = false) Boolean key) {
        return Result.success(planningService.positions(keyword, key));
    }

    @GetMapping("/positions/{id}")
    public Result<PositionVO> positionDetail(@PathVariable("id") Long id) {
        return Result.success(planningService.positionDetail(id));
    }

    @PutMapping("/positions/{id}/key")
    public Result<Void> updateKeyPosition(@PathVariable("id") Long id,
                                          @Valid @RequestBody KeyPositionUpdateDTO dto) {
        planningService.updateKeyPosition(id, dto.getKey());
        return Result.success();
    }

    @GetMapping("/key-positions")
    public Result<List<PositionVO>> keyPositions() {
        return Result.success(planningService.keyPositions());
    }

    @GetMapping("/succession")
    public Result<List<SuccessionCandidateVO>> succession(
            @RequestParam("positionId") Long positionId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return Result.success(planningService.succession(positionId, limit));
    }

    @PostMapping("/succession/refresh")
    public Result<List<SuccessionCandidateVO>> refreshSuccession(
            @RequestParam("positionId") Long positionId) {
        return Result.success("继任候选快照已刷新", planningService.refreshSuccession(positionId));
    }

    @GetMapping("/skill-coverage")
    public Result<List<SkillCoverageVO>> skillCoverage(@RequestParam("positionId") Long positionId) {
        return Result.success(planningService.skillCoverage(positionId));
    }

    @GetMapping("/talent-pools")
    public Result<List<TalentPoolVO>> talentPools() {
        return Result.success(planningService.talentPools());
    }

    @GetMapping("/talent-pools/{level}")
    public Result<TalentPoolVO> talentPool(@PathVariable("level") String level) {
        return Result.success(planningService.talentPool(level));
    }

    @PostMapping("/talent-pools/refresh")
    public Result<List<TalentPoolVO>> refreshTalentPools() {
        return Result.success("人才池统计已刷新", planningService.refreshTalentPools());
    }

    @GetMapping("/risks/employees")
    public Result<PageResult<RiskEmployeeVO>> employeeRisks(@ModelAttribute RiskQueryDTO query) {
        return Result.success(planningService.employeeRisks(query));
    }

    @GetMapping("/risks/employees/{id}")
    public Result<RiskEmployeeVO> employeeRisk(@PathVariable("id") Long id) {
        return Result.success(planningService.employeeRisk(id));
    }

    @PutMapping("/risks/employees/{id}/handle")
    public Result<Void> handleEmployeeRisk(@PathVariable("id") Long id,
                                           @Valid @RequestBody RiskHandleDTO dto) {
        planningService.handleEmployeeRisk(id, dto);
        return Result.success();
    }

    @GetMapping("/risks/positions")
    public Result<List<PositionRiskVO>> positionRisks() {
        return Result.success(planningService.positionRisks());
    }

    @PostMapping("/risks/positions/evaluate")
    public Result<List<PositionRiskVO>> evaluatePositionRisks() {
        return Result.success("岗位风险已重新评估", planningService.evaluatePositionRisks());
    }

    @GetMapping("/dashboard")
    public Result<PlanningDashboardVO> dashboard() {
        return Result.success(planningService.dashboard());
    }

    /** 跨服务读取员工档案；员工服务不可用时由 Feign fallback 返回 503。 */
    @GetMapping("/employee/{id}/profile")
    public Result<Map<String, Object>> employeeProfile(@PathVariable("id") Long id) {
        Result<Map<String, Object>> employee = employeeClient.getEmployee(id);
        if (employee == null || employee.getCode() != 200) {
            return Result.fail(employee == null ? 503 : employee.getCode(),
                    employee == null ? "员工服务暂不可用" : employee.getMessage());
        }
        return employee;
    }
}
