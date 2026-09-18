package com.talent.training.mapper;

import com.talent.training.vo.CourseItemVO;
import com.talent.training.vo.EmployeeTrainingProfileVO;
import com.talent.training.vo.TrainingEffectVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrainingQueryMapper {

    @Select("SELECT e.id AS employee_id, e.name AS employee_name, p.position_name "
            + "FROM emp_employee e LEFT JOIN position p ON p.id = e.position_id WHERE e.id = #{employeeId}")
    EmployeeTrainingProfileVO employeeProfile(@Param("employeeId") Long employeeId);

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
