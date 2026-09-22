package com.talent.analysis.service.impl;

import com.talent.analysis.mapper.AnalysisMapper;
import com.talent.analysis.service.AnalysisService;
import com.talent.analysis.vo.DashboardVO;
import com.talent.analysis.vo.FlowTrendVO;
import com.talent.analysis.vo.PipelineVO;
import com.talent.common.vo.NameValueVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模块五业务实现。
 */
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final AnalysisMapper analysisMapper;

    @Value("${talent.perf-year:2026}")
    private int perfYear;

    @Override
    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setTotalEmployees(analysisMapper.countEmployees());
        vo.setPositionCount(analysisMapper.countPositions());
        vo.setKeyPositionCount(analysisMapper.countKeyPositions());
        vo.setHighRiskCount(analysisMapper.countHighRisk());
        vo.setAvgSalary(analysisMapper.avgSalary());
        vo.setAvgTenure(analysisMapper.avgTenure());
        vo.setAvgAge(analysisMapper.avgAge());
        vo.setDepartmentDistribution(analysisMapper.departmentDistribution());
        vo.setPositionTop(analysisMapper.positionTop());
        vo.setTalentTagDistribution(analysisMapper.talentTagDistribution());
        vo.setPotentialDistribution(analysisMapper.potentialDistribution());
        vo.setWarningDistribution(analysisMapper.warningDistribution());
        vo.setPerfDistribution(analysisMapper.perfDistribution(perfYear));
        return vo;
    }

    @Override
    public PipelineVO pipeline() {
        PipelineVO vo = new PipelineVO();

        long total = nvl(analysisMapper.countEmployees());
        long highRisk = nvl(analysisMapper.countHighRisk());
        long middleRisk = nvl(analysisMapper.countMiddleRisk());
        long core = nvl(analysisMapper.countCoreTalent());
        long reserve = nvl(analysisMapper.countReserveTalent());
        long highPotential = nvl(analysisMapper.countHighPotential());
        Double avgPerf = analysisMapper.avgPerf(perfYear);

        vo.setTotalEmployees(total);
        vo.setPositionCount(analysisMapper.countPositions());
        vo.setKeyPositionCount(analysisMapper.countKeyPositions());
        vo.setHighRiskCount(highRisk);
        vo.setMiddleRiskCount(middleRisk);
        vo.setCoreTalentCount(core);
        vo.setReserveTalentCount(reserve);
        vo.setHighPotentialCount(highPotential);
        vo.setAvgAge(analysisMapper.avgAge());
        vo.setAvgTenure(analysisMapper.avgTenure());
        vo.setAvgSalary(analysisMapper.avgSalary());
        vo.setAvgPerf(avgPerf);

        double highRiskRate = total == 0 ? 0 : round1(highRisk * 100.0 / total);
        double middleRiskRate = total == 0 ? 0 : round1(middleRisk * 100.0 / total);
        double highPotentialRate = total == 0 ? 0 : round1(highPotential * 100.0 / total);
        double coreRate = total == 0 ? 0 : round1(core * 100.0 / total);

        vo.setHighRiskRate(highRiskRate);
        vo.setDepartmentDistribution(analysisMapper.departmentDistribution());
        vo.setLevelTierDistribution(analysisMapper.levelTierDistribution());
        vo.setPositionTypeDistribution(analysisMapper.positionTypeDistribution());
        vo.setAgeRangeDistribution(analysisMapper.ageRangeDistribution());
        vo.setTenureRangeDistribution(analysisMapper.tenureRangeDistribution());
        vo.setPotentialDistribution(analysisMapper.potentialDistribution());
        vo.setWarningDistribution(analysisMapper.warningDistribution());
        vo.setTalentTagDistribution(analysisMapper.talentTagDistribution());
        vo.setNineBox(buildNineBox());

        // 健康度评分：详见下方 scoreHealth 的算法说明
        double score = scoreHealth(highRiskRate, middleRiskRate, highPotentialRate, coreRate,
                avgPerf == null ? 0 : avgPerf);
        vo.setHealthScore(round1(score));
        vo.setHealthLevel(levelOf(score));
        vo.setHealthSuggestions(suggestions(highRiskRate, middleRiskRate, highPotentialRate, coreRate,
                avgPerf == null ? 0 : avgPerf));
        return vo;
    }

    @Override
    public FlowTrendVO flowTrend() {
        FlowTrendVO vo = new FlowTrendVO();
        vo.setJoinMonthly(analysisMapper.joinMonthly());
        vo.setJoinYearly(analysisMapper.joinYearly());
        vo.setMovementByType(analysisMapper.movementByType());
        vo.setTenureDistribution(analysisMapper.tenureRangeDistribution());
        vo.setRiskByTenure(analysisMapper.riskByTenure());
        vo.setCohorts(analysisMapper.cohortList(perfYear));
        vo.setRecentJoinCount(analysisMapper.countRecentJoin());
        vo.setTotalJoin(analysisMapper.countTotalJoin());
        vo.setLeaveCount(analysisMapper.countLeave());
        vo.setPromoteCount(analysisMapper.countPromote());

        List<NameValueVO> yearly = vo.getJoinYearly();
        if (yearly != null && !yearly.isEmpty()) {
            long sum = yearly.stream().mapToLong(item -> nvl(item.getValue())).sum();
            vo.setAvgAnnualJoin(round1(sum * 1.0 / yearly.size()));
        }
        return vo;
    }

    /* ------------------ 九宫格 ------------------ */

    private List<List<Integer>> buildNineBox() {
        List<List<Integer>> grid = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                row.add(0);
            }
            grid.add(row);
        }
        for (Map<String, Object> item : analysisMapper.nineBox(perfYear)) {
            int perf = toInt(item.get("perf_bucket"));
            int potential = toInt(item.get("potential_bucket"));
            int count = toInt(item.get("cnt"));
            if (perf >= 0 && perf < 3 && potential >= 0 && potential < 3) {
                grid.get(perf).set(potential, count);
            }
        }
        return grid;
    }

    /**
     * 梯队健康度：100 分制，四类扣分项，扣完为止。
     *
     * <pre>
     * 高风险占比     每 1% 扣 3 分，最多扣 30 分
     * 中风险占比     每 1% 扣 0.5 分，最多扣 15 分
     * 高潜人才占比   低于 20% 的部分，每 1% 扣 0.5 分，最多扣 20 分
     * 核心骨干占比   低于 15% 的部分，每 1% 扣 0.5 分，最多扣 15 分
     * 平均绩效       低于 3.5 的部分，每 0.1 分扣 2 分，最多扣 20 分
     * </pre>
     *
     * 这样算出来的分数只是给管理者一个横向可比的参考，不是学术指标，
     * 权重都写在代码里，团队可以按自己的管理口径调整。
     */
    private double scoreHealth(double highRiskRate, double middleRiskRate,
                               double highPotentialRate, double coreRate, double avgPerf) {
        double deduct = 0;
        deduct += Math.min(30, highRiskRate * 3);
        deduct += Math.min(15, middleRiskRate * 0.5);
        deduct += Math.min(20, Math.max(0, 20 - highPotentialRate) * 0.5);
        deduct += Math.min(15, Math.max(0, 15 - coreRate) * 0.5);
        deduct += Math.min(20, Math.max(0, 3.5 - avgPerf) * 20);
        return Math.max(0, Math.min(100, 100 - deduct));
    }

    private String levelOf(double score) {
        if (score >= 85) {
            return "优秀";
        }
        if (score >= 70) {
            return "良好";
        }
        if (score >= 60) {
            return "一般";
        }
        return "需要关注";
    }

    private List<String> suggestions(double highRiskRate, double middleRiskRate,
                                     double highPotentialRate, double coreRate, double avgPerf) {
        List<String> list = new ArrayList<>();
        if (highRiskRate >= 1) {
            list.add("高风险人员占比 " + highRiskRate + "%，建议对高风险名单逐人做留任面谈，重点看司龄 1-3 年的骨干");
        }
        if (middleRiskRate >= 10) {
            list.add("中风险人员占比 " + middleRiskRate + "%，建议结合薪酬系数和绩效做一次盘点，避免中风险转成高风险");
        }
        if (highPotentialRate < 20) {
            list.add("高潜（S/A 级）人才占比只有 " + highPotentialRate + "%，低于 20% 的储备线，建议加大高潜培养和外部引进");
        }
        if (coreRate < 15) {
            list.add("核心骨干占比 " + coreRate + "%，低于 15%，关键岗位继任储备偏薄");
        }
        if (avgPerf > 0 && avgPerf < 3.5) {
            list.add("平均绩效 " + avgPerf + " 分，低于 3.5，建议结合培训模块的技能缺口做针对性提升");
        }
        if (list.isEmpty()) {
            list.add("各项目前都在健康区间，保持现有节奏即可");
        }
        return list;
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private int toInt(Object value) {
        if (value == null) {
            return -1;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
