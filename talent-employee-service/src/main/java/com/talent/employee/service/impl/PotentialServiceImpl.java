package com.talent.employee.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import com.talent.employee.dto.PotentialSaveDTO;
import com.talent.employee.entity.Potential;
import com.talent.employee.mapper.PotentialMapper;
import com.talent.employee.service.PotentialService;
import com.talent.employee.vo.PotentialModelVO;
import com.talent.employee.vo.PotentialVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 潜力量化模型实现。
 *
 * <p>六个指标、权重合计 1.0；总分 = Σ(指标 × 权重)。
 * 等级：S ≥ 90，A ≥ 80，B ≥ 60，其余 C。
 */
@Service
@RequiredArgsConstructor
public class PotentialServiceImpl implements PotentialService {

    private static final Map<String, IndicatorDef> INDICATORS = new LinkedHashMap<>();

    static {
        INDICATORS.put("learningAgility", new IndicatorDef("学习敏锐度", "0.20", "快速学习新知识、适应新任务的能力"));
        INDICATORS.put("leadershipWillingness", new IndicatorDef("领导意愿", "0.15", "承担管理职责、带领团队的意愿"));
        INDICATORS.put("influence", new IndicatorDef("影响他人", "0.15", "跨团队沟通、推动他人达成共识的能力"));
        INDICATORS.put("resilience", new IndicatorDef("抗压韧性", "0.15", "面对压力和挫折时的稳定性"));
        INDICATORS.put("businessInsight", new IndicatorDef("业务洞察", "0.20", "理解业务、发现问题和机会的能力"));
        INDICATORS.put("achievementMotivation", new IndicatorDef("成就动机", "0.15", "追求高标准、持续改进的内驱力"));
    }

    private final PotentialMapper potentialMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PotentialModelVO model() {
        PotentialModelVO model = new PotentialModelVO();
        INDICATORS.forEach((key, def) -> model.getIndicators().add(
                new PotentialModelVO.Indicator(key, def.label(), new BigDecimal(def.weight()), def.description())));
        model.getLevels().add(new PotentialModelVO.LevelRule("S级（高潜）", new BigDecimal("90")));
        model.getLevels().add(new PotentialModelVO.LevelRule("A级（优秀）", new BigDecimal("80")));
        model.getLevels().add(new PotentialModelVO.LevelRule("B级（合格）", new BigDecimal("60")));
        model.getLevels().add(new PotentialModelVO.LevelRule("C级（待提升）", BigDecimal.ZERO));
        return model;
    }

    @Override
    public PotentialVO get(Long employeeId) {
        PotentialVO vo = potentialMapper.selectVO(employeeId);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "暂无该员工的潜力数据：" + employeeId);
        }
        vo.setIndicators(parseIndicators(vo.getIndicatorValues()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PotentialVO save(PotentialSaveDTO dto) {
        if (potentialMapper.countEmployee(dto.getEmployeeId()) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "员工不存在：" + dto.getEmployeeId());
        }
        upsert(dto.getEmployeeId(), dto.getIndicators());
        PotentialVO vo = potentialMapper.selectVO(dto.getEmployeeId());
        vo.setIndicators(parseIndicators(vo.getIndicatorValues()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSave(List<PotentialSaveDTO> records) {
        for (PotentialSaveDTO record : records) {
            if (potentialMapper.countEmployee(record.getEmployeeId()) == 0) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        "员工不存在：" + record.getEmployeeId());
            }
        }
        records.forEach(record -> upsert(record.getEmployeeId(), record.getIndicators()));
        return records.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int initModel(boolean force) {
        int count = 0;
        for (Potential potential : potentialMapper.selectAll()) {
            boolean empty = !StringUtils.hasText(potential.getIndicatorValues());
            if (!empty && !force) {
                continue;
            }
            BigDecimal base = potential.getTotalScore() == null ? new BigDecimal("60") : potential.getTotalScore();
            Map<String, BigDecimal> indicators = deriveIndicators(potential.getEmployeeId(), base);
            upsert(potential.getEmployeeId(), indicators);
            count++;
        }
        return count;
    }

    /* ---------------- 内部方法 ---------------- */

    private void upsert(Long employeeId, Map<String, BigDecimal> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "潜力指标不能为空");
        }
        for (Map.Entry<String, BigDecimal> entry : indicators.entrySet()) {
            if (!INDICATORS.containsKey(entry.getKey())) {
                throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "未知指标：" + entry.getKey());
            }
            BigDecimal value = entry.getValue();
            if (value == null || value.compareTo(BigDecimal.ZERO) < 0
                    || value.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                        "指标得分要在 0~100 之间：" + entry.getKey());
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, IndicatorDef> entry : INDICATORS.entrySet()) {
            BigDecimal value = indicators.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            total = total.add(value.multiply(new BigDecimal(entry.getValue().weight())));
        }
        total = total.setScale(1, RoundingMode.HALF_UP);

        Potential entity = new Potential();
        entity.setEmployeeId(employeeId);
        entity.setTotalScore(total);
        entity.setPotentialLevel(deriveLevel(total));
        entity.setIndicatorValues(toJson(indicators));

        Potential exists = potentialMapper.selectByEmployeeId(employeeId);
        if (exists == null) {
            potentialMapper.insert(entity);
        } else {
            entity.setId(exists.getId());
            potentialMapper.updateById(entity);
        }
    }

    /**
     * 初始化用的确定性指标推导：以现有潜力总分为基准，按员工ID产生稳定的小幅差异。
     * 相同员工重复执行结果一致，方便建模复现。
     */
    private Map<String, BigDecimal> deriveIndicators(Long employeeId, BigDecimal base) {
        String[] keys = INDICATORS.keySet().toArray(new String[0]);
        int[] offsets = {-4, 3, -2, 5, -3, 2};
        int shift = (int) (employeeId % 7) - 3;
        Map<String, BigDecimal> indicators = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            int offset = offsets[(i + (int) (employeeId % 6)) % offsets.length] + shift;
            BigDecimal value = base.add(BigDecimal.valueOf(offset))
                    .max(BigDecimal.ZERO).min(new BigDecimal("100"));
            indicators.put(keys[i], value.setScale(1, RoundingMode.HALF_UP));
        }
        return indicators;
    }

    private String deriveLevel(BigDecimal total) {
        if (total.compareTo(new BigDecimal("90")) >= 0) return "S级（高潜）";
        if (total.compareTo(new BigDecimal("80")) >= 0) return "A级（优秀）";
        if (total.compareTo(new BigDecimal("60")) >= 0) return "B级（合格）";
        return "C级（待提升）";
    }

    private Map<String, BigDecimal> parseIndicators(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Map<String, BigDecimal> indicators) {
        try {
            return objectMapper.writeValueAsString(indicators);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "潜力指标格式不正确");
        }
    }

    private record IndicatorDef(String label, String weight, String description) {
    }
}
