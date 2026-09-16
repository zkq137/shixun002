package com.talent.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.talent.training.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
