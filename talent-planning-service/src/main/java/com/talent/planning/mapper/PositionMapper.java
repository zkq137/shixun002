package com.talent.planning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.talent.planning.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PositionMapper extends BaseMapper<Position> {
    @Select(POSITION_SELECT + "level_tier AS positionLevel FROM position WHERE id = #{id}")
    Position selectByIdWithLevelTier(@Param("id") Long id);

    @Select(POSITION_SELECT + "position_level AS positionLevel FROM position WHERE id = #{id}")
    Position selectByIdWithPositionLevel(@Param("id") Long id);

    @Select("<script>" + POSITION_SELECT + "level_tier AS positionLevel FROM position "
            + POSITION_FILTER + " ORDER BY is_key DESC, level_tier ASC, id ASC</script>")
    List<Position> selectListWithLevelTier(@Param("keyword") String keyword, @Param("key") Boolean key);

    @Select("<script>" + POSITION_SELECT + "position_level AS positionLevel FROM position "
            + POSITION_FILTER + " ORDER BY is_key DESC, position_level ASC, id ASC</script>")
    List<Position> selectListWithPositionLevel(@Param("keyword") String keyword, @Param("key") Boolean key);

    @Update("UPDATE position SET is_key = #{key} WHERE id = #{id}")
    int updateKeyFlag(@Param("id") Long id, @Param("key") int key);

    String POSITION_SELECT = "SELECT id, position_name AS positionName, is_key AS isKey, created_at AS createdAt, ";
    String POSITION_FILTER = "<where>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + "position_name LIKE CONCAT('%', #{keyword}, '%')"
            + "</if>"
            + "<if test='key != null'>"
            + "<if test='keyword != null and keyword != \"\"'> AND </if>is_key = #{key}"
            + "</if>"
            + "</where>";
}
