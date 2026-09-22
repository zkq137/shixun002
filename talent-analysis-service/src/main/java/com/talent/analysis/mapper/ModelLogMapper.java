package com.talent.analysis.mapper;

import com.talent.analysis.vo.ModelLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模型训练日志：效果分析和日志溯源都读这张表。
 */
@Mapper
public interface ModelLogMapper {

    @Select("""
            <script>
            select id, model_name, model_type, version,
                   date_format(train_time, '%Y-%m-%d %H:%i') as train_time,
                   status, sample_count, train_duration_ms, metrics, params,
                   dataset_version, remark
            from model_log
            where 1 = 1
            <if test="modelName != null and modelName != ''">
                and model_name like concat('%', #{modelName}, '%')
            </if>
            <if test="status != null and status != ''">
                and status = #{status}
            </if>
            <if test="version != null and version != ''">
                and version like concat('%', #{version}, '%')
            </if>
            order by train_time desc, id desc
            limit #{offset}, #{size}
            </script>
            """)
    List<ModelLogVO> selectPage(@Param("modelName") String modelName,
                                @Param("status") String status,
                                @Param("version") String version,
                                @Param("offset") int offset,
                                @Param("size") int size);

    @Select("""
            <script>
            select count(*) from model_log
            where 1 = 1
            <if test="modelName != null and modelName != ''">
                and model_name like concat('%', #{modelName}, '%')
            </if>
            <if test="status != null and status != ''">
                and status = #{status}
            </if>
            <if test="version != null and version != ''">
                and version like concat('%', #{version}, '%')
            </if>
            </script>
            """)
    long countPage(@Param("modelName") String modelName,
                   @Param("status") String status,
                   @Param("version") String version);

    @Select("""
            select id, model_name, model_type, version,
                   date_format(train_time, '%Y-%m-%d %H:%i') as train_time,
                   status, sample_count, train_duration_ms, metrics, params,
                   dataset_version, remark
            from model_log
            where id = #{id}
            """)
    ModelLogVO selectById(@Param("id") Long id);

    @Select("""
            select id, model_name, model_type, version,
                   date_format(train_time, '%Y-%m-%d %H:%i') as train_time,
                   status, sample_count, train_duration_ms, metrics, params,
                   dataset_version, remark
            from model_log
            order by train_time desc, id desc
            limit #{limit}
            """)
    List<ModelLogVO> selectRecent(@Param("limit") int limit);

    @Select("""
            select distinct model_name from model_log order by model_name
            """)
    List<String> selectModelNames();
}
