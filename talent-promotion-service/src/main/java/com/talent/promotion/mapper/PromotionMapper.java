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
 *   <li>综合分 = 绩效 * 15 + 潜力评分 * 0.5 + 司龄 * 2，从高到低排。</li>
 * </ol>
 */
@Mapper
public interface PromotionMapper {

    @Select("select count(*) from position where id = #{id}")
    int countPosition(@Param("id") Long id);

    @Select("""
            select e.id as employee_id, e.emp_no, e.name, e.department,
                   e.level_tier, e.job_rank, e.tenure_years,
                   f.score as perf_score,
                   po.potential_level, po.total_score as potential_score,
                   w.warning_level,
                   round(ifnull(f.score, 0) * 15 + ifnull(po.total_score, 0) * 0.5 + e.tenure_years * 2, 1) as total_score
            from emp_employee e
            join position target on target.id = #{positionId}
            left join emp_performance f on f.employee_id = e.id and f.perf_year = #{perfYear}
            left join emp_potential po on po.employee_id = e.id
            left join resign_warning_record w on w.employee_id = e.id
            where e.status = '在职'
              and field(target.level_tier, '员工层', '主管层', '经理层', '总监层', '高管层', '决策层')
                  > field(e.level_tier, '员工层', '主管层', '经理层', '总监层', '高管层', '决策层')
              and ifnull(f.score, 0) >= 4
            order by total_score desc, po.total_score desc
            limit #{limit}
            """)
    List<PromotionCandidateVO> selectCandidates(@Param("positionId") Long positionId,
                                                @Param("perfYear") int perfYear,
                                                @Param("limit") int limit);
}
