package com.talent.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.talent.employee.entity.Performance;
import com.talent.employee.vo.PerformanceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 年度绩效查询。
 */
@Mapper
public interface PerformanceMapper extends BaseMapper<Performance> {

    @Select("""
            select p.id, p.employee_id, e.emp_no, e.name, e.department,
                   p.perf_year, p.score, p.grade, p.dimension_scores, p.created_at
            from emp_performance p
            join emp_employee e on e.id = p.employee_id
            where p.employee_id = #{employeeId}
            order by p.perf_year asc
            """)
    List<PerformanceVO> selectHistory(@Param("employeeId") Long employeeId);

    @Select("""
            select p.id, p.employee_id, e.emp_no, e.name, e.department,
                   p.perf_year, p.score, p.grade, p.dimension_scores, p.created_at
            from emp_performance p
            join emp_employee e on e.id = p.employee_id
            where p.employee_id = #{employeeId} and p.perf_year = #{year}
            limit 1
            """)
    PerformanceVO selectEmployeeYear(@Param("employeeId") Long employeeId, @Param("year") Integer year);

    @Select("select * from emp_performance where employee_id = #{employeeId} and perf_year = #{year} limit 1")
    Performance selectEntity(@Param("employeeId") Long employeeId, @Param("year") Integer year);

    @Select("select count(*) from emp_employee where id = #{id}")
    int countEmployee(@Param("id") Long id);

    @Select("select id from emp_employee where emp_no = #{empNo}")
    Long selectEmployeeIdByEmpNo(@Param("empNo") String empNo);

    @Select("""
            <script>
            select p.id, p.employee_id, e.emp_no, e.name, e.department,
                   p.perf_year, p.score, p.grade, p.dimension_scores, p.created_at
            from emp_performance p
            join emp_employee e on e.id = p.employee_id
            where 1 = 1
            <if test="employeeId != null"> and p.employee_id = #{employeeId} </if>
            order by e.emp_no, p.perf_year asc
            </script>
            """)
    List<PerformanceVO> selectForExport(@Param("employeeId") Long employeeId);
}
