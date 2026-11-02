package com.talent.analysis.service;

import com.talent.analysis.vo.ModelArtifactVO;
import com.talent.analysis.vo.ModelPredictionVO;
import com.talent.analysis.vo.ModelTrainResultVO;
import com.talent.analysis.vo.PositionRecommendationVO;

import java.util.List;

/**
 * 人才模型的训练、部署和在线推理。
 */
public interface TalentModelService {

    List<ModelTrainResultVO> trainAll();

    ModelTrainResultVO train(String modelCode);

    List<ModelArtifactVO> artifacts();

    ModelPredictionVO predictAttrition(String employeeIdentifier);

    ModelPredictionVO predictHighPotential(String employeeIdentifier);

    PositionRecommendationVO recommendPositions(String employeeIdentifier, int topN);
}
