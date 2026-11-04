package com.talent.training.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import com.talent.training.dto.TrainingAlgorithmSaveDTO;
import com.talent.training.entity.TrainingAlgorithm;
import com.talent.training.mapper.TrainingAlgorithmMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingAlgorithmService {
    private final TrainingAlgorithmMapper mapper;
    public List<TrainingAlgorithm> list() {
        return mapper.selectList(new LambdaQueryWrapper<TrainingAlgorithm>().orderByDesc(TrainingAlgorithm::getEnabled).orderByDesc(TrainingAlgorithm::getId));
    }
    public TrainingAlgorithm active() {
        return mapper.selectList(new LambdaQueryWrapper<TrainingAlgorithm>().eq(TrainingAlgorithm::getEnabled, true)).stream()
                .findFirst().orElseThrow(() -> invalid("请先新增并启用一个匹配算法配置"));
    }
    @Transactional
    public Long save(Long id, TrainingAlgorithmSaveDTO dto) {
        validate(dto);
        mapper.lockConfiguration();
        TrainingAlgorithm row = id == null ? new TrainingAlgorithm() : require(id);
        if (mapper.selectCount(new LambdaQueryWrapper<TrainingAlgorithm>().eq(TrainingAlgorithm::getName, dto.getName().trim()).ne(id != null, TrainingAlgorithm::getId, id)) > 0)
            throw invalid("算法方案名称已存在");
        if (Boolean.TRUE.equals(dto.getEnabled())) {
            mapper.update(null, new LambdaUpdateWrapper<TrainingAlgorithm>().eq(TrainingAlgorithm::getEnabled, true).set(TrainingAlgorithm::getEnabled, false));
        } else if (Boolean.TRUE.equals(row.getEnabled())) {
            throw invalid("请先启用另一个方案，再停用当前方案");
        }
        row.setName(dto.getName().trim()); row.setSkillWeight(dto.getSkillWeight());
        row.setPositionWeight(dto.getPositionWeight()); row.setGoalWeight(dto.getGoalWeight());
        row.setMatchThreshold(dto.getMatchThreshold()); row.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));
        if (id == null) mapper.insert(row); else mapper.updateById(row);
        return row.getId();
    }
    @Transactional
    public void delete(Long id) {
        mapper.lockConfiguration();
        if (Boolean.TRUE.equals(require(id).getEnabled())) throw invalid("请先启用另一个方案，再删除当前方案");
        mapper.deleteById(id);
    }
    private TrainingAlgorithm require(Long id) {
        TrainingAlgorithm row = mapper.selectById(id);
        if (row == null) throw invalid("算法方案不存在");
        return row;
    }
    static void validate(TrainingAlgorithmSaveDTO dto) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank() || dto.getName().trim().length() > 64) throw invalid("方案名称须为 1–64 个字符");
        for (BigDecimal value : new BigDecimal[]{dto.getSkillWeight(), dto.getPositionWeight(), dto.getGoalWeight(), dto.getMatchThreshold()})
            if (value == null || value.signum() < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0 || value.stripTrailingZeros().scale() > 2) throw invalid("权重与阈值须在 0–100 之间，最多两位小数");
        if (dto.getSkillWeight().add(dto.getPositionWeight()).add(dto.getGoalWeight()).compareTo(BigDecimal.valueOf(100)) != 0) throw invalid("三个匹配权重之和必须为 100");
    }
    private static BusinessException invalid(String message) { return new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), message); }
}
