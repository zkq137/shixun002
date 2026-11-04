package com.talent.training.mapper;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;
@Mapper
public interface TrainingReportMapper {
    @Select("SELECT '总体' AS dimension, '全部培训' AS name, COUNT(*) AS recordCount, COUNT(DISTINCT r.employee_id) AS employeeCount, "
          + "AVG(r.attend_rate) AS attendRate, AVG(r.pass_rate) AS passRate, AVG(r.improve_score) AS improveScore "
          + "FROM training_record r "
          + "UNION ALL SELECT '课程', c.course_name, COUNT(*), COUNT(DISTINCT r.employee_id), AVG(r.attend_rate), AVG(r.pass_rate), AVG(r.improve_score) "
          + "FROM training_record r JOIN course c ON c.id = r.course_id GROUP BY c.id, c.course_name "
          + "UNION ALL SELECT '部门', COALESCE(e.department, '未设置'), COUNT(*), COUNT(DISTINCT r.employee_id), AVG(r.attend_rate), AVG(r.pass_rate), AVG(r.improve_score) "
          + "FROM training_record r JOIN emp_employee e ON e.id = r.employee_id GROUP BY e.department "
          + "UNION ALL SELECT '岗位', COALESCE(p.position_name, '未设置'), COUNT(*), COUNT(DISTINCT r.employee_id), AVG(r.attend_rate), AVG(r.pass_rate), AVG(r.improve_score) "
          + "FROM training_record r JOIN emp_employee e ON e.id = r.employee_id LEFT JOIN position p ON p.id = e.position_id GROUP BY p.id, p.position_name "
          + "UNION ALL SELECT '分类', COALESCE(cat.category_name, '未分类'), COUNT(*), COUNT(DISTINCT r.employee_id), AVG(r.attend_rate), AVG(r.pass_rate), AVG(r.improve_score) "
          + "FROM training_record r JOIN course c ON c.id = r.course_id LEFT JOIN course_category cat ON cat.id = c.category_id GROUP BY cat.id, cat.category_name")
    List<Map<String, Object>> dimensions();
}
