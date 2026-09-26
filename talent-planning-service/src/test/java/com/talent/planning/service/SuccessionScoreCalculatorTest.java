package com.talent.planning.service;

import com.talent.planning.vo.CandidateSourceVO;
import com.talent.planning.vo.SuccessionCandidateVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuccessionScoreCalculatorTest {
    private final SuccessionScoreCalculator calculator = new SuccessionScoreCalculator();

    @Test
    void calculatesFourDimensionsWithFixedWeights() {
        CandidateSourceVO source = source(4, 5, "80", "90", 6);

        SuccessionCandidateVO result = calculator.calculate(source, List.of("架构设计"));

        assertEquals(new BigDecimal("80.00"), result.getSkillScore());
        assertEquals(new BigDecimal("80.00"), result.getMatchScore());
        assertEquals("READY_NOW", result.getReadiness());
        assertEquals(0, result.getPreparationMonths());
        assertEquals(List.of("架构设计"), result.getMissingSkills());
    }

    @Test
    void normalizesFivePointPerformanceToPercentage() {
        CandidateSourceVO source = source(1, 1, "4", "100", 0);

        SuccessionCandidateVO result = calculator.calculate(source, List.of());

        assertEquals(new BigDecimal("80.00"), result.getPerformanceScore());
        assertEquals(new BigDecimal("86.00"), result.getMatchScore());
    }

    @Test
    void usesZeroForMissingDataWithoutReweighting() {
        CandidateSourceVO source = source(0, 0, null, null, 4);

        SuccessionCandidateVO result = calculator.calculate(source, List.of());

        assertEquals(new BigDecimal("4.00"), result.getMatchScore());
        assertEquals("NOT_READY", result.getReadiness());
        assertTrue(result.getDataWarnings().contains("目标岗位未配置技能要求"));
        assertTrue(result.getDataWarnings().contains("缺少绩效数据"));
        assertTrue(result.getDataWarnings().contains("缺少潜力数据"));
    }

    @Test
    void appliesReadinessBoundaries() {
        assertEquals("READY_NOW", calculator.calculate(source(1, 1, "100", "100", 0), List.of()).getReadiness());
        assertEquals("READY_1_YEAR", calculator.calculate(source(1, 1, "50", "0", 0), List.of()).getReadiness());
        assertEquals("READY_2_YEARS", calculator.calculate(source(4, 5, "0", "0", 0), List.of()).getReadiness());
        assertEquals("NOT_READY", calculator.calculate(source(3, 5, "0", "0", 0), List.of()).getReadiness());
    }

    private CandidateSourceVO source(int matched, int required, String performance, String potential, int years) {
        CandidateSourceVO source = new CandidateSourceVO();
        source.setEmployeeId(1L);
        source.setEmpNo("E001");
        source.setEmployeeName("测试员工");
        source.setMatchedSkillCount(matched);
        source.setRequiredSkillCount(required);
        source.setPerformanceScore(performance == null ? null : new BigDecimal(performance));
        source.setPotentialScore(potential == null ? null : new BigDecimal(potential));
        source.setWorkYears(years);
        return source;
    }
}
