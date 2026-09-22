package com.talent.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.talent.employee.dto.EmployeeQuery;
import com.talent.employee.entity.Employee;
import com.talent.employee.vo.EmployeeDetailVO;
import com.talent.employee.vo.EmployeeVO;
import com.talent.employee.vo.PositionOptionVO;
import com.talent.common.vo.NameValueVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 员工模块的 Mapper。
 *
 * <p>增删改直接用 MyBatis-Plus 提供的 BaseMapper；查询涉及多表关联的，用注解写 SQL，
 * 动态条件放在 {@code <script>} 里。分页由 talent-common 里注册的分页插件接管。
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    /** 分页查询：基本信息 + 岗位名称 + 绩效/潜力/风险/标签 */
    @Select("""
            <script>
            select e.id, e.emp_no, e.name, e.gender, e.age, e.department, e.position_id,
                   p.position_name, e.level_tier, e.job_rank, e.manager_emp_no, e.work_mode,
                   e.tenure_years, e.status,
                   f.score as perf_score,
                   po.potential_level,
                   w.warning_level,
                   t.tag_name as talent_tag
            from emp_employee e
            left join position p on p.id = e.position_id
            left join emp_performance f on f.employee_id = e.id and f.perf_year = #{perfYear}
            left join emp_potential po on po.employee_id = e.id
            left join resign_warning_record w on w.employee_id = e.id
            left join emp_talent_tag t on t.employee_id = e.id
            where 1 = 1
            <if test="q.keyword != null and q.keyword != ''">
                and (e.name like concat('%', #{q.keyword}, '%') or e.emp_no like concat('%', #{q.keyword}, '%'))
            </if>
            <if test="q.department != null and q.department != ''">
                and e.department = #{q.department}
            </if>
            <if test="q.positionId != null">
                and e.position_id = #{q.positionId}
            </if>
            <if test="q.talentTag != null and q.talentTag != ''">
                and t.tag_name = #{q.talentTag}
            </if>
            <if test="q.warningLevel != null and q.warningLevel != ''">
                and w.warning_level = #{q.warningLevel}
            </if>
            <if test="q.status != null and q.status != ''">
                and e.status = #{q.status}
            </if>
            order by e.id
            </script>
            """)
    IPage<EmployeeVO> selectEmployeePage(IPage<EmployeeVO> page,
                                         @Param("q") EmployeeQuery query,
                                         @Param("perfYear") int perfYear);

    /** 员工详情（一个员工的全部字段 + 上级姓名 + 薪酬 + 绩效/潜力/风险） */
    @Select("""
            select e.id, e.emp_no, e.name, e.gender, e.age, e.department, e.position_id,
                   p.position_name, e.level_tier, e.job_rank, e.manager_emp_no, e.work_mode,
                   e.tenure_years, e.hire_date, e.education, e.phone, e.status,
                   f.score as perf_score,
                   po.potential_level, po.total_score as potential_score,
                   w.warning_level, w.risk_score,
                   t.tag_name as talent_tag,
                   s.base_salary, s.salary_coefficient,
                   m.name as manager_name
            from emp_employee e
            left join position p on p.id = e.position_id
            left join emp_performance f on f.employee_id = e.id and f.perf_year = #{perfYear}
            left join emp_potential po on po.employee_id = e.id
            left join resign_warning_record w on w.employee_id = e.id
            left join emp_talent_tag t on t.employee_id = e.id
            left join emp_salary s on s.employee_id = e.id
            left join emp_employee m on m.emp_no = e.manager_emp_no
            where e.id = #{id}
            """)
    EmployeeDetailVO selectEmployeeDetail(@Param("id") Long id, @Param("perfYear") int perfYear);

    /** 会哪些技能 */
    @Select("""
            select s.skill_name
            from emp_skill es
            join skill s on s.id = es.skill_id
            where es.employee_id = #{id}
            order by s.id
            """)
    List<String> selectSkillNames(@Param("id") Long id);

    /** 已完成哪些培训 */
    @Select("""
            select c.course_name
            from training_record tr
            join course c on c.id = tr.course_id
            where tr.employee_id = #{id}
            order by c.id
            """)
    List<String> selectCourseNames(@Param("id") Long id);

    /** 按部门统计人数 */
    @Select("""
            select department as name, count(*) as value
            from emp_employee
            group by department
            order by value desc
            """)
    List<NameValueVO> countByDepartment();

    /** 部门下拉列表 */
    @Select("select distinct department from emp_employee order by department")
    List<String> selectDepartments();

    /** 岗位下拉列表 */
    @Select("""
            select id, position_name, department, level_tier
            from position
            order by department, id
            """)
    List<PositionOptionVO> selectPositionOptions();

    /** 岗位是否存在 */
    @Select("select count(*) from position where id = #{id}")
    int countPosition(@Param("id") Long id);

    /** 工号是否已被占用（改的时候要排除自己） */
    @Select("""
            <script>
            select count(*) from emp_employee where emp_no = #{empNo}
            <if test="excludeId != null"> and id != #{excludeId} </if>
            </script>
            """)
    int countByEmpNo(@Param("empNo") String empNo, @Param("excludeId") Long excludeId);

    /** 按岗位名称找岗位（导入 Excel 时用名称换 id） */
    @Select("""
            <script>
            select id, position_name, department, level_tier
            from position
            where position_name = #{name}
            <if test="department != null and department != ''"> and department = #{department} </if>
            limit 1
            </script>
            """)
    PositionOptionVO selectPositionByName(@Param("name") String name,
                                          @Param("department") String department);

    /** 某员工会的技能（带分类，前端树控件回显用） */
    @Select("""
            select s.id, s.skill_name, s.parent_id, s.level, s.skill_category
            from emp_skill es
            join skill s on s.id = es.skill_id
            where es.employee_id = #{employeeId}
            order by s.skill_category, s.id
            """)
    List<com.talent.employee.vo.SkillVO> selectEmployeeSkills(@Param("employeeId") Long employeeId);

    /** 清空某员工的技能（改技能时先删后插） */
    @Delete("delete from emp_skill where employee_id = #{employeeId}")
    int deleteEmployeeSkills(@Param("employeeId") Long employeeId);

    /** 批量插入员工技能 */
    @Insert("""
            <script>
            insert into emp_skill (employee_id, skill_id) values
            <foreach collection="skillIds" item="sid" separator=",">
                (#{employeeId}, #{sid})
            </foreach>
            </script>
            """)
    int insertEmployeeSkills(@Param("employeeId") Long employeeId,
                             @Param("skillIds") List<Long> skillIds);

    /** 过滤出真实存在、且是叶子技能的 id */
    @Select("""
            <script>
            select id from skill where level = 2 and id in
            <foreach collection="ids" item="sid" open="(" separator="," close=")">#{sid}</foreach>
            </script>
            """)
    List<Long> selectValidSkillIds(@Param("ids") List<Long> ids);

    /** 技能名 -> 技能（导入时用名称换 id） */
    @Select("""
            <script>
            select id, skill_name from skill where level = 2 and skill_name in
            <foreach collection="names" item="n" open="(" separator="," close=")">#{n}</foreach>
            </script>
            """)
    List<com.talent.employee.vo.SkillVO> selectSkillsByNames(@Param("names") List<String> names);

    /** 一批员工的技能名（导出时一次查出来，避免每人查一次） */
    @Select("""
            <script>
            select es.employee_id, s.skill_name
            from emp_skill es
            join skill s on s.id = es.skill_id
            where es.employee_id in
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            order by es.employee_id, s.id
            </script>
            """)
    List<com.talent.employee.vo.EmployeeSkillNameVO> selectSkillNamesByEmployeeIds(@Param("ids") List<Long> ids);
}
