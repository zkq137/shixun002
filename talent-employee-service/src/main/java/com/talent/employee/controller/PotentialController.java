package com.talent.employee.controller;

import com.talent.common.result.Result;
import com.talent.employee.dto.PotentialBatchDTO;
import com.talent.employee.dto.PotentialSaveDTO;
import com.talent.employee.service.PotentialService;
import com.talent.employee.vo.PotentialModelVO;
import com.talent.employee.vo.PotentialVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模块一·潜力评估与量化建模。
 *
 * <p>直连：http://127.0.0.1:8081/api/potential/model
 * <p>网关：http://127.0.0.1:9090/api/potential/model
 */
@RestController
@RequestMapping("/api/potential")
@RequiredArgsConstructor
public class PotentialController {

    private final PotentialService potentialService;

    /** 量化模型定义（指标、权重、等级规则） */
    @GetMapping("/model")
    public Result<PotentialModelVO> model() {
        return Result.success(potentialService.model());
    }

    /** 初始化指标数据；force=true 覆盖已有指标 */
    @PostMapping("/model/init")
    public Result<Integer> initModel(
            @RequestParam(value = "force", defaultValue = "false") boolean force) {
        return Result.success("初始化完成", potentialService.initModel(force));
    }

    /** 查询某员工潜力 */
    @GetMapping("/employees/{employeeId}")
    public Result<PotentialVO> get(@PathVariable("employeeId") Long employeeId) {
        return Result.success(potentialService.get(employeeId));
    }

    /** 录入/覆盖某员工指标 */
    @PostMapping
    public Result<PotentialVO> save(@Valid @RequestBody PotentialSaveDTO dto) {
        return Result.success("潜力录入成功", potentialService.save(dto));
    }

    /** 批量录入 */
    @PostMapping("/batch")
    public Result<Integer> batchSave(@Valid @RequestBody PotentialBatchDTO dto) {
        return Result.success("批量录入成功", potentialService.batchSave(dto.getRecords()));
    }
}
