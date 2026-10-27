package com.talent.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.talent.employee.entity.Potential;
import com.talent.employee.vo.PotentialVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 潜力评估查询。
 */
@Mapper
public interface PotentialMapper extends BaseMapper<Potential> {

    @Select("""
            select p.id, p.employee_id, e.emp_no, e.name, e.department,
                   p.potential_level, p.total_score, p.indicator_values, p.updated_at
            from emp_potential p
            join emp_employee e on e.id = p.employee_id
            where p.employee_id = #{employeeId}
            limit 1
            """)
    PotentialVO selectVO(@Param("employeeId") Long employeeId);

    @Select("select * from emp_potential where employee_id = #{employeeId} limit 1")
    Potential selectByEmployeeId(@Param("employeeId") Long employeeId);

    @Select("select * from emp_potential")
    List<Potential> selectAll();

    @Select("select count(*) from emp_employee where id = #{id}")
    int countEmployee(@Param("id") Long id);
}
