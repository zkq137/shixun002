package com.talent.analysis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 岗位匹配模型推荐结果。
 */
@Data
public class PositionRecommendationVO {

    private Long employeeId;
    private String employeeName;
    private String modelVersion;
    private List<Item> recommendations = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long positionId;
        private String positionName;
        private String department;
        private Double score;
    }
}
