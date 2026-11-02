package com.talent.analysis.controller;

import com.talent.analysis.service.TalentModelService;
import com.talent.analysis.vo.ModelArtifactVO;
import com.talent.analysis.vo.ModelPredictionVO;
import com.talent.analysis.vo.ModelTrainResultVO;
import com.talent.analysis.vo.PositionRecommendationVO;
import com.talent.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 可训练、可部署的人才模型接口。
 */
@RestController
@RequestMapping("/api/analysis/model")
@RequiredArgsConstructor
public class TalentModelController {

    private final TalentModelService talentModelService;

    @PostMapping("/train-all")
    public Result<List<ModelTrainResultVO>> trainAll() {
        return Result.success("模型训练完成", talentModelService.trainAll());
    }

    @PostMapping("/train/{modelCode}")
    public Result<ModelTrainResultVO> train(@PathVariable("modelCode") String modelCode) {
        return Result.success("模型训练完成", talentModelService.train(modelCode));
    }

    @GetMapping("/artifacts")
    public Result<List<ModelArtifactVO>> artifacts() {
        return Result.success(talentModelService.artifacts());
    }

    @GetMapping("/predict/attrition/{employeeId}")
    public Result<ModelPredictionVO> predictAttrition(@PathVariable("employeeId") String employeeIdentifier) {
        return Result.success(talentModelService.predictAttrition(employeeIdentifier));
    }

    @GetMapping("/predict/potential/{employeeId}")
    public Result<ModelPredictionVO> predictPotential(@PathVariable("employeeId") String employeeIdentifier) {
        return Result.success(talentModelService.predictHighPotential(employeeIdentifier));
    }

    @GetMapping("/recommend/positions/{employeeId}")
    public Result<PositionRecommendationVO> recommendPositions(
            @PathVariable("employeeId") String employeeIdentifier,
            @RequestParam(value = "topN", defaultValue = "5") int topN) {
        return Result.success(talentModelService.recommendPositions(employeeIdentifier, topN));
    }
}
