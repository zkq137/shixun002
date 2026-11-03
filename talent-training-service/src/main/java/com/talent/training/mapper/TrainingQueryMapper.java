package com.talent.training.mapper;

import com.talent.training.vo.CourseItemVO;
import com.talent.training.vo.EmployeeTrainingProfileVO;
import com.talent.training.vo.TrainingEmployeeReferenceVO;
import com.talent.training.vo.TrainingEffectVO;
import com.talent.training.vo.TrainingPositionReferenceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface TrainingQueryMapper {

    @Select("SELECT e.id AS employee_id, e.emp_no, e.name AS employee_name, e.department, "
            + "p.position_name, e.level_tier, e.job_rank, e.tenure_years, e.status, "
            + "(SELECT f.score FROM emp_performance f WHERE f.employee_id = e.id "
            + "ORDER BY f.perf_year DESC LIMIT 1) AS perf_score, "
            + "(SELECT po.potential_level FROM emp_potential po WHERE po.employee_id = e.id LIMIT 1) AS potential_level, "
            + "(SELECT w.warning_level FROM resign_warning_record w WHERE w.employee_id = e.id "
            + "ORDER BY w.warning_time DESC LIMIT 1) AS warning_level, "
            + "(SELECT t.tag_name FROM emp_talent_tag t WHERE t.employee_id = e.id LIMIT 1) AS talent_tag "
            + "FROM emp_employee e LEFT JOIN position p ON p.id = e.position_id WHERE e.id = #{employeeId}")
    EmployeeTrainingProfileVO employeeProfile(@Param("employeeId") Long employeeId);

    /** 按姓名或工号查找问题中提到的员工，供培训助手构建只读上下文。 */
    @Select("SELECT e.id, e.emp_no, e.name, e.department "
            + "FROM emp_employee e "
            + "WHERE #{text} LIKE CONCAT('%', e.name, '%') "
            + "OR #{text} LIKE CONCAT('%', e.emp_no, '%') "
            + "ORDER BY CHAR_LENGTH(e.emp_no) DESC, CHAR_LENGTH(e.name) DESC, e.id "
            + "LIMIT 50")
    List<TrainingEmployeeReferenceVO> employeeReferences(@Param("text") String text);

    /** 查找问题中提到的岗位。 */
    @Select("SELECT id, position_name, department FROM position "
            + "WHERE #{text} LIKE CONCAT('%', position_name, '%') "
            + "ORDER BY CHAR_LENGTH(position_name) DESC, id LIMIT 10")
    List<TrainingPositionReferenceVO> positionReferences(@Param("text") String text);

    /** 岗位级必备培训。 */
    @Select("SELECT c.id, c.course_name, c.course_type, c.for_position, c.difficulty, c.duration, "
            + "FALSE AS completed FROM pos_course_require req "
            + "JOIN course c ON c.id = req.course_id "
            + "WHERE req.position_id = #{positionId} ORDER BY c.id")
    List<CourseItemVO> requiredCoursesByPosition(@Param("positionId") Long positionId);

    /** 岗位级核心/加分技能要求。 */
    @Select("SELECT s.skill_name AS skillName, req.require_type AS requireType "
            + "FROM pos_skill_require req JOIN skill s ON s.id = req.skill_id "
            + "WHERE req.position_id = #{positionId} ORDER BY req.require_type, s.id")
    List<Map<String, Object>> skillRequirementsByPosition(@Param("positionId") Long positionId);

    @Select("SELECT c.id, c.course_name, c.course_type, c.for_position, c.difficulty, c.duration, "
            + "TRUE AS completed, r.attend_rate, r.pass_rate, r.improve_score, r.created_at AS completed_at "
            + "FROM training_record r JOIN course c ON c.id = r.course_id "
            + "WHERE r.employee_id = #{employeeId} ORDER BY r.created_at DESC")
    List<CourseItemVO> completedCourses(@Param("employeeId") Long employeeId);

    @Select("SELECT DISTINCT c.id, c.course_name, c.course_type, c.for_position, c.difficulty, c.duration, "
            + "FALSE AS completed FROM emp_employee e "
            + "JOIN pos_course_require req ON req.position_id = e.position_id "
            + "JOIN course c ON c.id = req.course_id "
            + "LEFT JOIN training_record r ON r.employee_id = e.id AND r.course_id = c.id "
            + "WHERE e.id = #{employeeId} AND r.id IS NULL ORDER BY c.difficulty, c.id")
    List<CourseItemVO> requiredButIncompleteCourses(@Param("employeeId") Long employeeId);

    @Select("SELECT DISTINCT s.skill_name FROM emp_employee e "
            + "JOIN pos_skill_require req ON req.position_id = e.position_id "
            + "JOIN skill s ON s.id = req.skill_id "
            + "LEFT JOIN emp_skill own ON own.employee_id = e.id AND own.skill_id = req.skill_id "
            + "WHERE e.id = #{employeeId} AND own.id IS NULL ORDER BY s.skill_name")
    List<String> skillGaps(@Param("employeeId") Long employeeId);

    @Select("SELECT c.id AS course_id, c.course_name, COUNT(r.id) AS participant_count, "
            + "ROUND(AVG(r.attend_rate), 2) AS average_attend_rate, "
            + "ROUND(AVG(r.pass_rate), 2) AS average_pass_rate, "
            + "ROUND(AVG(r.improve_score), 2) AS average_improve_score "
            + "FROM course c LEFT JOIN training_record r ON r.course_id = c.id "
            + "GROUP BY c.id, c.course_name ORDER BY participant_count DESC, c.id")
    List<TrainingEffectVO> trainingEffects();

    @Select("SELECT (SELECT COUNT(*) FROM training_record WHERE course_id = #{courseId}) "
            + "+ (SELECT COUNT(*) FROM path_task WHERE course_id = #{courseId}) "
            + "+ (SELECT COUNT(*) FROM pos_course_require WHERE course_id = #{courseId})")
    long courseReferenceCount(@Param("courseId") Long courseId);
}
