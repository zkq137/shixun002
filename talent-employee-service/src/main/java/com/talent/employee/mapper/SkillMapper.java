package com.talent.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.talent.employee.entity.Skill;
import com.talent.employee.vo.SkillVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 技能树的 Mapper。
 *
 * <p>统计「多少员工会」「多少岗位要求」用的是相关子查询，172 个技能跑起来没有压力；
 * 数据量真上来了再改成 join + group by 或加缓存。
 */
@Mapper
public interface SkillMapper extends BaseMapper<Skill> {

    /** 全部节点（平铺），带引用次数，按分类顺序排 */
    @Select("""
            select s.id, s.skill_name, s.parent_id, s.level, s.sort_order, s.status,
                   s.description, s.skill_category,
                   (select count(*) from emp_skill es where es.skill_id = s.id) as employee_count,
                   (select count(*) from pos_skill_require r where r.skill_id = s.id) as position_count
            from skill s
            order by s.parent_id, s.sort_order, s.id
            """)
    List<SkillVO> selectAllWithRefCount();

    /** 按关键字搜技能（名称或分类名） */
    @Select("""
            select s.id, s.skill_name, s.parent_id, s.level, s.sort_order, s.status,
                   s.description, s.skill_category,
                   (select count(*) from emp_skill es where es.skill_id = s.id) as employee_count,
                   (select count(*) from pos_skill_require r where r.skill_id = s.id) as position_count
            from skill s
            where s.skill_name like concat('%', #{keyword}, '%')
               or s.skill_category like concat('%', #{keyword}, '%')
            order by s.level, s.parent_id, s.sort_order, s.id
            """)
    List<SkillVO> search(@Param("keyword") String keyword);

    /** 某个分类下的技能 */
    @Select("""
            select s.id, s.skill_name, s.parent_id, s.level, s.sort_order, s.status,
                   s.description, s.skill_category,
                   (select count(*) from emp_skill es where es.skill_id = s.id) as employee_count,
                   (select count(*) from pos_skill_require r where r.skill_id = s.id) as position_count
            from skill s
            where s.parent_id = #{parentId}
            order by s.sort_order, s.id
            """)
    List<SkillVO> selectByParent(@Param("parentId") Long parentId);

    /** 同名技能是否已存在（改名时排除自己） */
    @Select("""
            <script>
            select count(*) from skill where skill_name = #{skillName}
            <if test="excludeId != null"> and id != #{excludeId} </if>
            </script>
            """)
    int countByName(@Param("skillName") String skillName, @Param("excludeId") Long excludeId);

    /** 有几个子节点 */
    @Select("select count(*) from skill where parent_id = #{id}")
    int countChildren(@Param("id") Long id);

    /** 有多少员工会这个技能 */
    @Select("select count(*) from emp_skill where skill_id = #{id}")
    int countEmployeeRef(@Param("id") Long id);

    /** 有多少岗位要求这个技能 */
    @Select("select count(*) from pos_skill_require where skill_id = #{id}")
    int countPositionRef(@Param("id") Long id);

    /** 前端树控件要的分类列表 */
    @Select("""
            select s.id, s.skill_name, s.parent_id, s.level, s.sort_order, s.status, s.skill_category,
                   (select count(*) from skill c where c.parent_id = s.id) as child_count
            from skill s
            where s.parent_id = 0
            order by s.sort_order, s.id
            """)
    List<SkillVO> selectCategories();
}
