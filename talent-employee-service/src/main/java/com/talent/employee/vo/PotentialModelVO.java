package com.talent.employee.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 潜力量化模型定义：指标、权重、等级规则、计算公式。
 */
@Data
public class PotentialModelVO {

    private String modelName = "员工潜力量化模型";
    private String formula = "总分 = Σ(指标得分 × 权重)，指标得分范围 0~100";
    private List<Indicator> indicators = new ArrayList<>();
    private List<LevelRule> levels = new ArrayList<>();

    @Data
    @AllArgsConstructor
    public static class Indicator {
        private String key;
        private String label;
        private BigDecimal weight;
        private String description;
    }

    @Data
    @AllArgsConstructor
    public static class LevelRule {
        private String level;
        private BigDecimal minScore;
    }
}
