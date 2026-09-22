package com.talent.analysis.mapper;

import com.talent.common.vo.NameValueVO;
import com.talent.analysis.vo.FlowTrendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 模块五的 Mapper：整库做聚合统计。
 */
@Mapper
public interface AnalysisMapper {

    @Select("select count(*) from emp_employee where status = '在职'")
    Long countEmployees();

    @Select("select count(*) from position")
    Long countPositions();

    @Select("select count(*) from position where is_key = 1")
    Long countKeyPositions();

    @Select("select count(*) from resign_warning_record where warning_level = '高风险'")
    Long countHighRisk();

    @Select("select round(avg(base_salary), 2) from emp_salary")
    Double avgSalary();

    @Select("select round(avg(tenure_years), 1) from emp_employee")
    Double avgTenure();

    @Select("select round(avg(age), 1) from emp_employee")
    Double avgAge();

    @Select("""
            select department as name, count(*) as value
            from emp_employee
            group by department
            order by value desc
            """)
    List<NameValueVO> departmentDistribution();

    @Select("""
            select p.position_name as name, count(*) as value
            from emp_employee e
            join position p on p.id = e.position_id
            group by p.id
            order by value desc
            limit 10
            """)
    List<NameValueVO> positionTop();

    @Select("""
            select tag_name as name, count(*) as value
            from emp_talent_tag
            group by tag_name
            order by value desc
            """)
    List<NameValueVO> talentTagDistribution();

    @Select("""
            select ifnull(potential_level, '未评估') as name, count(*) as value
            from emp_potential
            group by potential_level
            order by value desc
            """)
    List<NameValueVO> potentialDistribution();

    @Select("""
            select ifnull(warning_level, '未评估') as name, count(*) as value
            from resign_warning_record
            group by warning_level
            order by value desc
            """)
    List<NameValueVO> warningDistribution();

    @Select("""
            select concat(cast(score as signed), ' 分') as name, count(*) as value
            from emp_performance
            where perf_year = #{perfYear}
            group by score
            order by score desc
            """)
    List<NameValueVO> perfDistribution(@Param("perfYear") int perfYear);

    /* ==================== 梯队整体状态 ==================== */

    @Select("select count(*) from emp_talent_tag where tag_name = '核心骨干'")
    Long countCoreTalent();

    @Select("select count(*) from emp_talent_tag where tag_name = '储备人才'")
    Long countReserveTalent();

    @Select("select count(*) from emp_potential where potential_level in ('S级（高潜）', 'A级（优秀）')")
    Long countHighPotential();

    @Select("select count(*) from resign_warning_record where warning_level = '中风险'")
    Long countMiddleRisk();

    @Select("select round(avg(score), 2) from emp_performance where perf_year = #{perfYear}")
    Double avgPerf(@Param("perfYear") int perfYear);

    @Select("""
            select e.level_tier as name, count(*) as value
            from emp_employee e
            where e.status = '在职' and e.level_tier is not null
            group by e.level_tier
            order by field(e.level_tier, '决策层', '高管层', '总监层', '经理层', '主管层', '员工层')
            """)
    List<NameValueVO> levelTierDistribution();

    @Select("""
            select p.position_type as name, count(*) as value
            from emp_employee e
            join position p on p.id = e.position_id
            where e.status = '在职' and p.position_type is not null
            group by p.position_type
            order by value desc
            """)
    List<NameValueVO> positionTypeDistribution();

    @Select("""
            select t.age_range as name, count(*) as value
            from (
                select case
                         when e.age < 25 then '25岁以下'
                         when e.age < 30 then '25-29岁'
                         when e.age < 35 then '30-34岁'
                         when e.age < 40 then '35-39岁'
                         when e.age < 50 then '40-49岁'
                         else '50岁以上'
                       end as age_range
                from emp_employee e
                where e.status = '在职' and e.age is not null
            ) t
            group by t.age_range
            order by field(t.age_range, '25岁以下', '25-29岁', '30-34岁', '35-39岁', '40-49岁', '50岁以上')
            """)
    List<NameValueVO> ageRangeDistribution();

    @Select("""
            select t.tenure_range as name, count(*) as value
            from (
                select case
                         when e.tenure_years < 1 then '1年以内'
                         when e.tenure_years < 3 then '1-3年'
                         when e.tenure_years < 5 then '3-5年'
                         when e.tenure_years < 10 then '5-10年'
                         else '10年以上'
                       end as tenure_range
                from emp_employee e
                where e.status = '在职'
            ) t
            group by t.tenure_range
            order by field(t.tenure_range, '1年以内', '1-3年', '3-5年', '5-10年', '10年以上')
            """)
    List<NameValueVO> tenureRangeDistribution();

    /** 九宫格：绩效(低/中/高) × 潜力(低/中/高)，返回 perf_bucket / potential_bucket / cnt */
    @Select("""
            select case when ifnull(f.score, 0) >= 4 then 2 when f.score >= 3 then 1 else 0 end as perf_bucket,
                   case when po.potential_level in ('S级（高潜）', 'A级（优秀）') then 2
                        when po.potential_level = 'B级（合格）' then 1
                        else 0 end as potential_bucket,
                   count(*) as cnt
            from emp_employee e
            left join emp_performance f on f.employee_id = e.id and f.perf_year = #{perfYear}
            left join emp_potential po on po.employee_id = e.id
            where e.status = '在职'
            group by perf_bucket, potential_bucket
            """)
    List<Map<String, Object>> nineBox(@Param("perfYear") int perfYear);

    /* ==================== 人才流动趋势 ==================== */

    @Select("""
            select date_format(effect_date, '%Y-%m') as name, count(*) as value
            from emp_movement
            where movement_type = '入职' and effect_date >= date_sub(curdate(), interval 24 month)
            group by name
            order by name
            """)
    List<NameValueVO> joinMonthly();

    @Select("""
            select date_format(effect_date, '%Y') as name, count(*) as value
            from emp_movement
            where movement_type = '入职'
            group by name
            order by name
            """)
    List<NameValueVO> joinYearly();

    @Select("""
            select movement_type as name, count(*) as value
            from emp_movement
            group by movement_type
            order by value desc
            """)
    List<NameValueVO> movementByType();

    /** 各司龄段的高风险人数：看哪个阶段最容易流失 */
    @Select("""
            select t.tenure_range as name,
                   sum(case when t.warning_level = '高风险' then 1 else 0 end) as value
            from (
                select case
                         when e.tenure_years < 1 then '1年以内'
                         when e.tenure_years < 3 then '1-3年'
                         when e.tenure_years < 5 then '3-5年'
                         when e.tenure_years < 10 then '5-10年'
                         else '10年以上'
                       end as tenure_range,
                       w.warning_level
                from emp_employee e
                left join resign_warning_record w on w.employee_id = e.id
                where e.status = '在职'
            ) t
            group by t.tenure_range
            order by field(t.tenure_range, '1年以内', '1-3年', '3-5年', '5-10年', '10年以上')
            """)
    List<NameValueVO> riskByTenure();

    /** 按入职年份看每一批人的现状 */
    @Select("""
            select date_format(m.effect_date, '%Y') as hire_year,
                   count(distinct e.id) as employee_count,
                   round(avg(e.tenure_years), 1) as avg_tenure,
                   round(avg(f.score), 2) as avg_perf,
                   sum(case when w.warning_level = '高风险' then 1 else 0 end) as high_risk_count,
                   round(100 * sum(case when w.warning_level = '高风险' then 1 else 0 end)
                         / count(distinct e.id), 1) as high_risk_rate,
                   sum(case when t.tag_name = '核心骨干' then 1 else 0 end) as core_talent_count
            from emp_movement m
            join emp_employee e on e.id = m.employee_id
            left join emp_performance f on f.employee_id = e.id and f.perf_year = #{perfYear}
            left join resign_warning_record w on w.employee_id = e.id
            left join emp_talent_tag t on t.employee_id = e.id
            where m.movement_type = '入职'
            group by hire_year
            order by hire_year desc
            limit 12
            """)
    List<FlowTrendVO.CohortVO> cohortList(@Param("perfYear") int perfYear);

    @Select("""
            select count(*) from emp_movement
            where movement_type = '入职' and effect_date >= date_sub(curdate(), interval 12 month)
            """)
    Long countRecentJoin();

    @Select("select count(*) from emp_movement where movement_type = '入职'")
    Long countTotalJoin();

    @Select("select count(*) from emp_movement where movement_type = '离职'")
    Long countLeave();

    @Select("select count(*) from emp_movement where movement_type = '晋升'")
    Long countPromote();
}
