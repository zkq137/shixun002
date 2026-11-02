package com.talent.promotion.service.impl;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromotionEvidenceServiceTest {

    @Test
    void resolvesPositionByFullNameAndExplicitId() {
        List<Map<String, Object>> positions = List.of(
                position(7, "市场经理"),
                position(8, "高级市场经理"));

        assertThat(PromotionEvidenceService.resolvePosition("评估市场经理岗位", positions))
                .extracting(position -> position.get("id"))
                .isEqualTo(7L);
        assertThat(PromotionEvidenceService.resolvePosition("评估岗位ID 8 的候选人", positions))
                .extracting(position -> position.get("id"))
                .isEqualTo(8L);
    }

    @Test
    void ranksFullEvidenceFirstAndKeepsPromotionOnlySeparate() {
        Map<String, Object> promotionReady = promotion(1, "候选人甲", 120, "低风险");
        Map<String, Object> promotionOnly = promotion(2, "候选人乙", 150, "低风险");
        Map<String, Object> promotionNotReady = promotion(3, "候选人丙", 130, "中风险");
        Map<String, Object> readySuccessor = succession(1, "READY_NOW", 80, 75, 3);
        Map<String, Object> notReadySuccessor = succession(3, "READY_2_YEARS", 70, 30, 1);

        List<Map<String, Object>> ranked = PromotionEvidenceService.rankCandidates(
                List.of(promotionReady, promotionOnly, promotionNotReady),
                List.of(readySuccessor, notReadySuccessor));

        assertThat(ranked.get(0))
                .containsEntry("name", "候选人甲")
                .containsEntry("evidenceCoverage", "full")
                .containsEntry("recommendedAction", "优先进入晋升评审");
        assertThat(ranked.get(1))
                .containsEntry("name", "候选人丙")
                .containsEntry("recommendedAction", "进入继任池，暂不晋升");
        assertThat(ranked.get(2))
                .containsEntry("name", "候选人乙")
                .containsEntry("evidenceCoverage", "promotion_only")
                .containsEntry("recommendedAction", "待补充继任评估，不直接下晋升结论");
    }

    private static Map<String, Object> position(long id, String name) {
        Map<String, Object> position = new LinkedHashMap<>();
        position.put("id", id);
        position.put("positionName", name);
        return position;
    }

    private static Map<String, Object> promotion(long id, String name, double totalScore,
                                                   String warningLevel) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("employeeId", id);
        candidate.put("name", name);
        candidate.put("totalScore", totalScore);
        candidate.put("warningLevel", warningLevel);
        return candidate;
    }

    private static Map<String, Object> succession(long id, String readiness, double matchScore,
                                                   double skillScore, int matchedSkillCount) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("employeeId", id);
        candidate.put("readiness", readiness);
        candidate.put("matchScore", matchScore);
        candidate.put("skillScore", skillScore);
        candidate.put("matchedSkillCount", matchedSkillCount);
        candidate.put("requiredSkillCount", 5);
        candidate.put("missingSkills", List.of("运营管理"));
        return candidate;
    }
}
