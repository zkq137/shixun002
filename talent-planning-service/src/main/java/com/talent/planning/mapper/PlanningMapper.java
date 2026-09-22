package com.talent.planning.mapper;

import com.talent.planning.vo.RiskEmployeeVO;
import com.talent.planning.vo.SuccessionCandidateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模块二的 Mapper：岗位、技能要求、流失风险都在这个库里，直接查。
 */
@Mapper
public interface PlanningMapper {

    @Select("select count(*) from position where id = #{id}")
    int countPosition(@Param("id") Long id);

    @Select("select position_name from position where id = #{id}")
    String selectPositionName(@Param("id") Long id);

    /**
     * 继任候选人：拿目标岗位的「核心技能」去比候选人的技能，
     * 匹配度 = 候选人具备的核心技能数 / 岗位要求的核心技能数。
     * 已经在目标岗位上的人不再作为候选。
     */
    @Select("""
            select c.employee_id, c.emp_no, c.name, c.department, c.job_rank, c.level_tier,
                   c.tenure_years, c.perf_score, c.potential_level, c.warning_level,
                   c.core_require, c.matched,
                   round(c.matched * 100 / nullif(c.core_require, 0), 1) as match_score
            from (
                select e.id as employee_id, e.emp_no, e.name, e.department, e.job_rank,
                       e.level_tier, e.tenure_years,
                       f.score as perf_score,
                       po.potential_level,
                       w.warning_level,
                       (select count(distinct r.skill_id) from pos_skill_require r
                         where r.position_id = #{positionId} and r.require_type = '核心') as core_require,
                       (select count(distinct es.skill_id) from pos_skill_require r
                          join emp_skill es on es.skill_id = r.skill_id and es.employee_id = e.id
                         where r.position_id = #{positionId} and r.require_type = '核心') as matched
                from emp_employee e
                left join emp_performance f on f.employee_id = e.id and f.perf_year = #{perfYear}
                left join emp_potential po on po.employee_id = e.id
                left join resign_warning_record w on w.employee_id = e.id
                where e.position_id != #{positionId} and e.status = '在职'
            ) c
            order by c.matched desc, c.perf_score desc, c.tenure_years desc
            limit #{limit}
            """)
    List<SuccessionCandidateVO> selectSuccessionCandidates(@Param("positionId") Long positionId,
                                                           @Param("perfYear") int perfYear,
                                                           @Param("limit") int limit);

    /** 流失风险最高的 N 个人 */
    @Select("""
            select e.id as employee_id, e.emp_no, e.name, e.department, e.job_rank,
                   w.risk_score, w.warning_level, po.potential_level, t.tag_name as talent_tag
            from resign_warning_record w
            join emp_employee e on e.id = w.employee_id
            left join emp_potential po on po.employee_id = e.id
            left join emp_talent_tag t on t.employee_id = e.id
            order by w.risk_score desc
            limit #{limit}
            """)
    List<RiskEmployeeVO> selectTopRiskEmployees(@Param("limit") int limit);

    /** 某个岗位要求的核心技能，以及全公司有多少人具备（用来找共性缺口） */
    @Select("""
            select s.skill_name as name,
                   (select count(distinct es.employee_id) from emp_skill es where es.skill_id = s.id) as value
            from pos_skill_require r
            join skill s on s.id = r.skill_id
            where r.position_id = #{positionId} and r.require_type = '核心'
            order by value asc
            """)
    java.util.List<com.talent.common.vo.NameValueVO> selectSkillCoverage(@Param("positionId") Long positionId);
}
