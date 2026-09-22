package com.talent.analysis.controller;

import com.talent.analysis.service.ModelLogService;
import com.talent.analysis.vo.ModelEffectVO;
import com.talent.analysis.vo.ModelLogVO;
import com.talent.common.result.PageResult;
import com.talent.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模型运行效果分析与日志溯源。
 *
 * <p>直连：http://127.0.0.1:8085/api/analysis/model/effect
 * <p>网关：http://127.0.0.1:9090/api/analysis/model/effect
 */
@RestController
@RequestMapping("/api/analysis/model")
@RequiredArgsConstructor
public class ModelLogController {

    private final ModelLogService modelLogService;

    /** 效果分析：训练次数、成功率、平均/最好 auc、分模型汇总、版本趋势 */
    @GetMapping("/effect")
    public Result<ModelEffectVO> effect() {
        return Result.success(modelLogService.effect());
    }

    /** 训练日志分页查询：按模型名、状态、版本筛选（溯源入口） */
    @GetMapping("/logs")
    public Result<PageResult<ModelLogVO>> logs(@RequestParam(value = "modelName", required = false) String modelName,
                                               @RequestParam(value = "status", required = false) String status,
                                               @RequestParam(value = "version", required = false) String version,
                                               @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                               @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(modelLogService.page(modelName, status, version, pageNum, pageSize));
    }

    /** 某次训练的完整信息：训练参数、评估指标、数据版本、备注 */
    @GetMapping("/logs/{id}")
    public Result<ModelLogVO> logDetail(@PathVariable("id") Long id) {
        return Result.success(modelLogService.detail(id));
    }

    /** 模型名列表，给筛选下拉用 */
    @GetMapping("/names")
    public Result<List<String>> names() {
        return Result.success(modelLogService.modelNames());
    }
}
