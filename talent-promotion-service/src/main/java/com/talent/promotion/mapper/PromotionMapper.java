package com.talent.promotion.mapper;

import com.talent.promotion.vo.PromotionCandidateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模块四的 Mapper。
 *
 * <p>晋升规则（简化版，都可以按自己的业务改）：
 * <ol>
 *   <li>候选人职级层级必须低于目标岗位（用 FIELD 把层级排成 <员工层, 主管层, 经理层, 总监层, 高管层, 决策层>）；</li>
 *   <li>绩效要达到岗位要求（岗位画像里写的是"绩效4分以上"，这里按 4 分过滤）；</li>
 *   <li>人才基础分 = 绩效 * 15 + 潜力评分 * 0.5 + 司龄 * 2，归一化后按 60% 计入综合分；</li>
 *   <li>岗位匹配分 = 核心技能匹配 80% + 加分技能匹配 20%，按 40% 计入综合分。</li>
 * </ol>
 */
@Mapper
public interface PromotionMapper {

    String CANDIDATE_SELECT = """
            select c.employee_id, c.emp_no, c.name, c.department,
                   c.level_tier, c.job_rank, c.tenure_years,
                   c.perf_score, c.potential_level, c.potential_score, c.warning_level,
                   c.talent_score, c.skill_match_score,
                   c.matched_core_skill_count, c.required_core_skill_count,
                   round(c.talent_score * 0.6 + c.skill_match_score * 0.4, 1) as total_score
            from (
                select e.id as employee_id, e.emp_no, e.name, e.department,
                       e.level_tier, e.job_rank, e.tenure_years,
                       f.score as perf_score,
                       po.potential_level, po.total_score as potential_score,
                       w.warning_level,
                       round(least(100,
                           (ifnull(f.score, 0) * 15 + ifnull(po.total_score, 0) * 0.5
                            + ifnull(e.tenure_years, 0) * 2) * 100 / 145
                       ), 1) as talent_score,
                       (select count(distinct req.skill_id)
                        from pos_skill_require req
                        where req.position_id = #{positionId} and req.require_type = '核心') as required_core_skill_count,
                       (select count(distinct own.skill_id)
                        from pos_skill_require req
                        join emp_skill own on own.skill_id = req.skill_id and own.employee_id = e.id
                        where req.position_id = #{positionId} and req.require_type = '核心') as matched_core_skill_count,
                       round(
                           ifnull((select count(distinct own.skill_id)
                                   from pos_skill_require req
                                   join emp_skill own on own.skill_id = req.skill_id and own.employee_id = e.id
                                   where req.position_id = #{positionId} and req.require_type = '核心')
                                  * 100 / nullif((select count(distinct req.skill_id)
                                   from pos_skill_require req
                                   where req.position_id = #{positionId} and req.require_type = '核心'), 0), 0) * 0.8
                           + ifnull((select count(distinct own.skill_id)
                                   from pos_skill_require req
                                   join emp_skill own on own.skill_id = req.skill_id and own.employee_id = e.id
                                   where req.position_id = #{positionId} and req.require_type = '加分')
                                  * 100 / nullif((select count(distinct req.skill_id)
                                   from pos_skill_require req
                                   where req.position_id = #{positionId} and req.require_type = '加分'), 0), 0) * 0.2
                       , 1) as skill_match_score
                from emp_employee e
                join position target on target.id = #{positionId}
                left join emp_performance f on f.employee_id = e.id and f.perf_year = #{perfYear}
                left join emp_potential po on po.employee_id = e.id
                left join resign_warning_record w on w.employee_id = e.id
                where e.status = '在职'
                  and field(target.level_tier, '员工层', '主管层', '经理层', '总监层', '高管层', '决策层')
                      > field(e.level_tier, '员工层', '主管层', '经理层', '总监层', '高管层', '决策层')
                  and ifnull(f.score, 0) >= 4
            ) c
            """;

    @Select("select count(*) from position where id = #{id}")
    int countPosition(@Param("id") Long id);

    @Select(CANDIDATE_SELECT + " order by total_score desc, c.skill_match_score desc, c.talent_score desc, c.employee_id limit #{limit}")
    List<PromotionCandidateVO> selectCandidates(@Param("positionId") Long positionId,
                                                @Param("perfYear") int perfYear,
                                                @Param("limit") int limit);

    /** 绩效潜力矩阵使用完整候选池，不按排行上限截断。 */
    @Select(CANDIDATE_SELECT + " order by total_score desc, c.skill_match_score desc, c.talent_score desc, c.employee_id")
    List<PromotionCandidateVO> selectMatrixCandidates(@Param("positionId") Long positionId,
                                                      @Param("perfYear") int perfYear);
}
