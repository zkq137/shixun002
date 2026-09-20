package com.talent.planning.service;

import com.talent.planning.vo.CandidateSourceVO;
import com.talent.planning.vo.SuccessionCandidateVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class SuccessionScoreCalculator {
    public SuccessionCandidateVO calculate(CandidateSourceVO source, List<String> missingSkills) {
        BigDecimal skill = source.getRequiredSkillCount() == null || source.getRequiredSkillCount() == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(source.getMatchedSkillCount() == null ? 0 : source.getMatchedSkillCount())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(source.getRequiredSkillCount()), 2, RoundingMode.HALF_UP);
        BigDecimal performance = clamp(source.getPerformanceScore());
        BigDecimal potential = clamp(source.getPotentialScore());
        BigDecimal experience = BigDecimal.valueOf(Math.min(100,
                Math.max(0, (source.getWorkYears() == null ? 0 : source.getWorkYears()) * 10L))).setScale(2);
        BigDecimal total = skill.multiply(new BigDecimal("0.50"))
                .add(performance.multiply(new BigDecimal("0.20")))
                .add(potential.multiply(new BigDecimal("0.20")))
                .add(experience.multiply(new BigDecimal("0.10"))).setScale(2, RoundingMode.HALF_UP);

        SuccessionCandidateVO vo = new SuccessionCandidateVO();
        vo.setEmployeeId(source.getEmployeeId());
        vo.setEmpNo(source.getEmpNo());
        vo.setEmployeeName(source.getEmployeeName());
        vo.setDepartment(source.getDepartment());
        vo.setCurrentPosition(source.getCurrentPosition());
        vo.setWorkYears(source.getWorkYears());
        vo.setSkillScore(skill);
        vo.setPerformanceScore(performance);
        vo.setPotentialScore(potential);
        vo.setExperienceScore(experience);
        vo.setMatchScore(total);
        vo.setMatchedSkillCount(source.getMatchedSkillCount() == null ? 0 : source.getMatchedSkillCount());
        vo.setRequiredSkillCount(source.getRequiredSkillCount() == null ? 0 : source.getRequiredSkillCount());
        vo.setMissingSkills(missingSkills == null ? List.of() : missingSkills);
        applyReadiness(vo, total);
        List<String> warnings = new ArrayList<>();
        if (source.getRequiredSkillCount() == null || source.getRequiredSkillCount() == 0) warnings.add("目标岗位未配置技能要求");
        if (source.getPerformanceScore() == null) warnings.add("缺少绩效数据");
        if (source.getPotentialScore() == null) warnings.add("缺少潜力数据");
        vo.setDataWarnings(warnings);
        vo.setRecommendationReason(String.format("技能 %.2f、绩效 %.2f、潜力 %.2f、经验 %.2f，按 50%%/20%%/20%%/10%% 加权",
                skill, performance, potential, experience));
        return vo;
    }

    private void applyReadiness(SuccessionCandidateVO vo, BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) {
            vo.setReadiness("READY_NOW"); vo.setPreparationMonths(0);
        } else if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            vo.setReadiness("READY_1_YEAR"); vo.setPreparationMonths(12);
        } else if (score.compareTo(BigDecimal.valueOf(40)) >= 0) {
            vo.setReadiness("READY_2_YEARS"); vo.setPreparationMonths(24);
        } else {
            vo.setReadiness("NOT_READY"); vo.setPreparationMonths(36);
        }
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO.setScale(2);
        return value.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
}
