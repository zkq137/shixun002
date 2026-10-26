package com.talent.planning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.PageResult;
import com.talent.common.result.ResultCode;
import com.talent.planning.dto.RiskHandleDTO;
import com.talent.planning.dto.RiskQueryDTO;
import com.talent.planning.entity.*;
import com.talent.planning.mapper.*;
import com.talent.planning.service.PlanningService;
import com.talent.planning.service.SuccessionScoreCalculator;
import com.talent.planning.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PlanningServiceImpl implements PlanningService {
    private static final Set<String> HANDLE_STATUSES = Set.of("未处理", "处理中", "已处理");
    private final PositionMapper positionMapper;
    private final SuccessionCandidateMapper candidateMapper;
    private final TalentPoolMapper talentPoolMapper;
    private final PositionRiskMapper positionRiskMapper;
    private final ResignWarningMapper resignWarningMapper;
    private final PlanningQueryMapper queryMapper;
    private final SuccessionScoreCalculator scoreCalculator;
    private volatile Boolean employeeHasPositionId;

    @Override
    public List<PositionVO> positions(String keyword, Boolean key) {
        return positionMapper.selectList(new LambdaQueryWrapper<Position>()
                .like(StringUtils.hasText(keyword), Position::getPositionName, keyword)
                .eq(key != null, Position::getIsKey, Boolean.TRUE.equals(key) ? 1 : 0)
                .orderByDesc(Position::getIsKey).orderByAsc(Position::getPositionLevel).orderByAsc(Position::getId))
                .stream().map(p -> toPositionVO(p, false)).toList();
    }

    @Override public PositionVO positionDetail(Long id) { return toPositionVO(requirePosition(id), true); }

    @Override
    public void updateKeyPosition(Long id, boolean key) {
        Position position = requirePosition(id);
        position.setIsKey(key ? 1 : 0);
        positionMapper.updateById(position);
    }

    @Override
    public List<PositionVO> keyPositions() {
        return positionMapper.selectList(new LambdaQueryWrapper<Position>().eq(Position::getIsKey, 1)
                .orderByAsc(Position::getPositionLevel).orderByAsc(Position::getId))
                .stream().map(p -> toPositionVO(p, false)).toList();
    }

    @Override
    public List<SuccessionCandidateVO> succession(Long positionId, int limit) {
        requirePosition(positionId);
        if (limit < 1 || limit > 100) throw validation("limit 必须在 1 到 100 之间");
        return calculateCandidates(positionId).stream().limit(limit).toList();
    }

    @Override
    @Transactional
    public List<SuccessionCandidateVO> refreshSuccession(Long positionId) {
        requirePosition(positionId);
        List<SuccessionCandidateVO> result = calculateCandidates(positionId);
        candidateMapper.delete(new LambdaQueryWrapper<SuccessionCandidate>().eq(SuccessionCandidate::getPositionId, positionId));
        LocalDateTime now = LocalDateTime.now();
        for (SuccessionCandidateVO vo : result) {
            SuccessionCandidate entity = new SuccessionCandidate();
            entity.setPositionId(positionId); entity.setEmployeeId(vo.getEmployeeId()); entity.setMatchScore(vo.getMatchScore());
            entity.setSkillScore(vo.getSkillScore()); entity.setPerformanceScore(vo.getPerformanceScore());
            entity.setPotentialScore(vo.getPotentialScore()); entity.setExperienceScore(vo.getExperienceScore());
            entity.setReadiness(vo.getReadiness()); entity.setPreparationMonths(vo.getPreparationMonths());
            entity.setMissingSkills(String.join("、", vo.getMissingSkills()));
            entity.setRecommendationReason(vo.getRecommendationReason());
            entity.setDataWarnings(String.join("、", vo.getDataWarnings())); entity.setCalculatedAt(now);
            candidateMapper.insert(entity);
        }
        return result;
    }

    @Override
    public List<SkillCoverageVO> skillCoverage(Long positionId) {
        requirePosition(positionId);
        List<SkillCoverageVO> result = queryMapper.skillCoverage(positionId);
        result.forEach(item -> {
            int required = value(item.getRequiredCount()), qualified = value(item.getQualifiedCount());
            item.setGapCount(Math.max(0, required - qualified));
            item.setCoverageRate(percent(qualified, required));
        });
        return result;
    }

    @Override
    public List<TalentPoolVO> talentPools() {
        return talentPoolMapper.selectList(new LambdaQueryWrapper<TalentPool>().orderByAsc(TalentPool::getLevel))
                .stream().map(this::toTalentPoolVO).toList();
    }

    @Override
    public TalentPoolVO talentPool(String level) {
        if (!StringUtils.hasText(level)) throw validation("人才池层级不能为空");
        TalentPool pool = talentPoolMapper.selectOne(new LambdaQueryWrapper<TalentPool>().eq(TalentPool::getLevel, level));
        if (pool == null) throw notFound("人才池不存在");
        return toTalentPoolVO(pool);
    }

    @Override
    @Transactional
    public List<TalentPoolVO> refreshTalentPools() {
        Map<String, List<Long>> byLevel = new LinkedHashMap<>();
        for (Position p : positionMapper.selectList(new LambdaQueryWrapper<Position>().eq(Position::getIsKey, 1))) {
            String level = StringUtils.hasText(p.getPositionLevel()) ? p.getPositionLevel() : "未分级";
            byLevel.computeIfAbsent(level, ignored -> new ArrayList<>()).add(p.getId());
        }
        talentPoolMapper.delete(null);
        for (Map.Entry<String, List<Long>> entry : byLevel.entrySet()) {
            List<SuccessionCandidate> candidates = candidateMapper.selectList(new LambdaQueryWrapper<SuccessionCandidate>()
                    .in(SuccessionCandidate::getPositionId, entry.getValue()));
            int readyNow = distinctEmployees(candidates, "READY_NOW");
            int readyOneYear = distinctEmployees(candidates, "READY_1_YEAR");
            int current = (int) candidates.stream().filter(c -> Set.of("READY_NOW", "READY_1_YEAR").contains(c.getReadiness()))
                    .map(SuccessionCandidate::getEmployeeId).distinct().count();
            TalentPool pool = new TalentPool();
            pool.setLevel(entry.getKey()); pool.setTotalCapacity(entry.getValue().size()); pool.setCurrentCount(current);
            pool.setGapCount(Math.max(0, entry.getValue().size() - current)); pool.setReadyNowCount(readyNow);
            pool.setReadyOneYearCount(readyOneYear); pool.setUpdatedAt(LocalDateTime.now()); talentPoolMapper.insert(pool);
        }
        return talentPools();
    }

    @Override
    public PageResult<RiskEmployeeVO> employeeRisks(RiskQueryDTO query) {
        if (query == null) query = new RiskQueryDTO();
        validatePage(query.getPageNum(), query.getPageSize());
        long offset = (query.getPageNum() - 1) * query.getPageSize();
        List<RiskEmployeeVO> records = hasEmployeePositionId()
                ? queryMapper.riskEmployeesByPositionId(query.getWarningLevel(), query.getHandleStatus(), query.getDepartment(), query.getKeyword(), offset, query.getPageSize())
                : queryMapper.riskEmployeesByPositionName(query.getWarningLevel(), query.getHandleStatus(), query.getDepartment(), query.getKeyword(), offset, query.getPageSize());
        long total = queryMapper.riskEmployeeCount(query.getWarningLevel(), query.getHandleStatus(), query.getDepartment(), query.getKeyword());
        return new PageResult<>(total, query.getPageNum(), query.getPageSize(), records);
    }

    @Override
    public RiskEmployeeVO employeeRisk(Long id) {
        RiskEmployeeVO vo = hasEmployeePositionId() ? queryMapper.riskDetailByPositionId(id) : queryMapper.riskDetailByPositionName(id);
        if (vo == null) throw notFound("流失预警不存在");
        return vo;
    }

    @Override
    public void handleEmployeeRisk(Long id, RiskHandleDTO dto) {
        ResignWarningRecord record = resignWarningMapper.selectById(id);
        if (record == null) throw notFound("流失预警不存在");
        if (dto == null || !HANDLE_STATUSES.contains(dto.getHandleStatus())) throw validation("处理状态只能是未处理、处理中或已处理");
        if (!StringUtils.hasText(dto.getHandler())) throw validation("处理人不能为空");
        record.setHandleStatus(dto.getHandleStatus()); record.setHandler(dto.getHandler().trim());
        record.setHandleRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
        record.setHandledAt("未处理".equals(dto.getHandleStatus()) ? null : LocalDateTime.now());
        resignWarningMapper.updateById(record);
    }

    @Override
    public List<PositionRiskVO> positionRisks() {
        Map<String, Integer> riskOrder = Map.of("高", 0, "中", 1, "低", 2);
        return positionRiskMapper.selectList(new LambdaQueryWrapper<PositionRisk>()
                        .orderByDesc(PositionRisk::getCheckedAt)).stream()
                .sorted(Comparator.comparingInt(r -> riskOrder.getOrDefault(r.getRiskLevel(), 3)))
                .map(this::toPositionRiskVO).toList();
    }

    @Override
    @Transactional
    public List<PositionRiskVO> evaluatePositionRisks() {
        positionRiskMapper.delete(null);
        for (Position position : positionMapper.selectList(null)) {
            int incumbents = incumbentCount(position.getId()), highRisk = highRiskCount(position.getId());
            List<SuccessionCandidate> all = candidateMapper.selectList(new LambdaQueryWrapper<SuccessionCandidate>()
                    .eq(SuccessionCandidate::getPositionId, position.getId()));
            List<SuccessionCandidate> qualified = all.stream().filter(c -> !"NOT_READY".equals(c.getReadiness())).toList();
            int readyNow = (int) all.stream().filter(c -> "READY_NOW".equals(c.getReadiness())).count();
            List<SkillCoverageVO> skillCoverage = skillCoverage(position.getId());
            BigDecimal coverage = averageCoverage(skillCoverage);
            boolean key = value(position.getIsKey()) == 1;
            List<String> reasons = new ArrayList<>();
            if (incumbents == 0) reasons.add("岗位当前无人");
            if (qualified.isEmpty()) reasons.add("无合格继任人");
            if (key && readyNow == 0) reasons.add("关键岗位无可立即继任人");
            if (highRisk > 0) reasons.add("在岗人员存在高流失风险");
            if (skillCoverage.isEmpty()) reasons.add("岗位未配置技能要求");
            else if (coverage.compareTo(BigDecimal.valueOf(60)) < 0) reasons.add("技能覆盖率低于60%");
            String level = incumbents == 0 || (key && readyNow == 0) || highRisk > 0 ? "高"
                    : qualified.isEmpty() || !skillCoverage.isEmpty()
                    && coverage.compareTo(BigDecimal.valueOf(60)) < 0 ? "中" : "低";
            if (reasons.isEmpty()) reasons.add("岗位继任储备和技能覆盖正常");
            PositionRisk risk = new PositionRisk();
            risk.setPositionId(position.getId()); risk.setRiskLevel(level); risk.setRiskDesc(String.join("；", reasons));
            risk.setRiskScope(key ? "关键岗位" : "普通岗位"); risk.setIncumbentCount(incumbents);
            risk.setSuccessorCount(qualified.size()); risk.setReadyNowCount(readyNow); risk.setHighRiskEmployeeCount(highRisk);
            risk.setSkillCoverageRate(coverage); risk.setCheckedAt(LocalDateTime.now()); positionRiskMapper.insert(risk);
        }
        return positionRisks();
    }

    @Override
    public PlanningDashboardVO dashboard() {
        long positions = positionMapper.selectCount(null);
        List<Position> keys = positionMapper.selectList(new LambdaQueryWrapper<Position>().eq(Position::getIsKey, 1));
        long covered = keys.stream().filter(p -> candidateMapper.selectCount(new LambdaQueryWrapper<SuccessionCandidate>()
                .eq(SuccessionCandidate::getPositionId, p.getId()).ne(SuccessionCandidate::getReadiness, "NOT_READY")) > 0).count();
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (String readiness : List.of("READY_NOW", "READY_1_YEAR", "READY_2_YEARS", "NOT_READY"))
            distribution.put(readiness, candidateMapper.selectCount(new LambdaQueryWrapper<SuccessionCandidate>().eq(SuccessionCandidate::getReadiness, readiness)));
        List<TalentPoolVO> pools = talentPools();
        PlanningDashboardVO vo = new PlanningDashboardVO();
        vo.setPositionCount(positions); vo.setKeyPositionCount((long) keys.size()); vo.setCoveredKeyPositionCount(covered);
        vo.setKeyPositionCoverageRate(percent(covered, keys.size())); vo.setUncoveredKeyPositionCount(Math.max(0, keys.size() - covered));
        vo.setReadyNowCandidateCount(distribution.get("READY_NOW")); vo.setTotalTalentGap(pools.stream().mapToLong(p -> value(p.getGapCount())).sum());
        vo.setHighRiskEmployeeCount(resignWarningMapper.selectCount(new LambdaQueryWrapper<ResignWarningRecord>().eq(ResignWarningRecord::getWarningLevel, "高")));
        vo.setUnhandledWarningCount(resignWarningMapper.selectCount(new LambdaQueryWrapper<ResignWarningRecord>().eq(ResignWarningRecord::getHandleStatus, "未处理")));
        vo.setHighRiskPositions(positionRisks().stream().filter(r -> "高".equals(r.getRiskLevel())).toList());
        vo.setReadinessDistribution(distribution); vo.setTalentPoolGaps(pools); return vo;
    }

    private List<SuccessionCandidateVO> calculateCandidates(Long positionId) {
        List<CandidateSourceVO> sources = hasEmployeePositionId() ? queryMapper.candidatesByPositionId(positionId)
                : queryMapper.candidatesByPositionName(positionId);
        return sources.stream().map(s -> scoreCalculator.calculate(s, queryMapper.missingSkills(positionId, s.getEmployeeId())))
                .sorted((a, b) -> b.getMatchScore().compareTo(a.getMatchScore())).toList();
    }

    private PositionVO toPositionVO(Position p, boolean includeSkills) {
        PositionVO vo = new PositionVO();
        vo.setId(p.getId()); vo.setPositionName(p.getPositionName()); vo.setPositionLevel(p.getPositionLevel()); vo.setKey(value(p.getIsKey()) == 1);
        vo.setIncumbentCount(incumbentCount(p.getId()));
        vo.setSuccessorCount(candidateMapper.selectCount(new LambdaQueryWrapper<SuccessionCandidate>().eq(SuccessionCandidate::getPositionId, p.getId()).ne(SuccessionCandidate::getReadiness, "NOT_READY")).intValue());
        vo.setReadyNowCount(candidateMapper.selectCount(new LambdaQueryWrapper<SuccessionCandidate>().eq(SuccessionCandidate::getPositionId, p.getId()).eq(SuccessionCandidate::getReadiness, "READY_NOW")).intValue());
        vo.setSuccessionCoverageRate(vo.getKey() ? (vo.getSuccessorCount() > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO) : null);
        PositionRisk risk = positionRiskMapper.selectOne(new LambdaQueryWrapper<PositionRisk>().eq(PositionRisk::getPositionId, p.getId()).last("LIMIT 1"));
        if (risk != null) { vo.setRiskLevel(risk.getRiskLevel()); vo.setRiskDescription(risk.getRiskDesc()); }
        if (includeSkills) vo.setSkills(skillCoverage(p.getId())); return vo;
    }

    private PositionRiskVO toPositionRiskVO(PositionRisk r) {
        Position p = positionMapper.selectById(r.getPositionId()); PositionRiskVO vo = new PositionRiskVO();
        vo.setId(r.getId()); vo.setPositionId(r.getPositionId()); vo.setPositionName(p == null ? null : p.getPositionName());
        vo.setPositionLevel(p == null ? null : p.getPositionLevel()); vo.setKey(p != null && value(p.getIsKey()) == 1);
        vo.setRiskLevel(r.getRiskLevel()); vo.setRiskDescription(r.getRiskDesc()); vo.setRiskScope(r.getRiskScope());
        vo.setIncumbentCount(r.getIncumbentCount()); vo.setSuccessorCount(r.getSuccessorCount()); vo.setReadyNowCount(r.getReadyNowCount());
        vo.setHighRiskEmployeeCount(r.getHighRiskEmployeeCount()); vo.setSkillCoverageRate(r.getSkillCoverageRate()); vo.setCheckedAt(r.getCheckedAt()); return vo;
    }

    private TalentPoolVO toTalentPoolVO(TalentPool p) {
        TalentPoolVO vo = new TalentPoolVO(); vo.setLevel(p.getLevel()); vo.setTotalCapacity(value(p.getTotalCapacity()));
        vo.setCurrentCount(value(p.getCurrentCount())); vo.setGapCount(value(p.getGapCount())); vo.setCoverageRate(percent(vo.getCurrentCount(), vo.getTotalCapacity()));
        vo.setReadyNowCount(value(p.getReadyNowCount())); vo.setReadyOneYearCount(value(p.getReadyOneYearCount())); vo.setUpdatedAt(p.getUpdatedAt()); return vo;
    }

    private BigDecimal averageCoverage(List<SkillCoverageVO> list) {
        if (list.isEmpty()) return BigDecimal.ZERO;
        return list.stream().map(SkillCoverageVO::getCoverageRate).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP);
    }
    private boolean hasEmployeePositionId() {
        if (employeeHasPositionId == null) synchronized (this) { if (employeeHasPositionId == null) employeeHasPositionId = queryMapper.columnExists("emp_employee", "position_id") > 0; }
        return employeeHasPositionId;
    }
    private int incumbentCount(Long id) { return hasEmployeePositionId() ? queryMapper.incumbentCountByPositionId(id) : queryMapper.incumbentCountByPositionName(id); }
    private int highRiskCount(Long id) { return hasEmployeePositionId() ? queryMapper.highRiskByPositionId(id) : queryMapper.highRiskByPositionName(id); }
    private int distinctEmployees(List<SuccessionCandidate> list, String status) { return (int) list.stream().filter(c -> status.equals(c.getReadiness())).map(SuccessionCandidate::getEmployeeId).distinct().count(); }
    private Position requirePosition(Long id) { Position p = id == null ? null : positionMapper.selectById(id); if (p == null) throw notFound("岗位不存在"); return p; }
    private void validatePage(long page, long size) { if (page < 1 || page > 1_000_000 || size < 1 || size > 100) throw validation("分页参数不合法，pageNum 在 1 到 1000000 之间且 pageSize 在 1 到 100 之间"); }
    private BigDecimal percent(long a, long b) { return b == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(b), 2, RoundingMode.HALF_UP); }
    private int value(Integer n) { return n == null ? 0 : n; }
    private BusinessException validation(String msg) { return new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), msg); }
    private BusinessException notFound(String msg) { return new BusinessException(ResultCode.NOT_FOUND.getCode(), msg); }
}
