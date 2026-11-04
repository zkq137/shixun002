package com.talent.training.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.talent.training.entity.TrainingAlgorithm;
import org.apache.ibatis.annotations.*;
@Mapper
public interface TrainingAlgorithmMapper extends BaseMapper<TrainingAlgorithm> {
    @Select("SELECT id FROM training_algorithm_lock WHERE id = 1 FOR UPDATE")
    Integer lockConfiguration();
}
