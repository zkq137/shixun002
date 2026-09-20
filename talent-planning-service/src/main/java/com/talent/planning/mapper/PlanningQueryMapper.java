package com.talent.planning.mapper;

import com.talent.planning.vo.CandidateSourceVO;
import com.talent.planning.vo.RiskEmployeeVO;
import com.talent.planning.vo.SkillCoverageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PlanningQueryMapper {
    @Select("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
            + "AND TABLE_NAME = #{tableName} AND COLUMN_NAME = #{columnName}")
    int columnExists(@Param("tableName") String tableName, @Param("columnName") String columnName);

    @Select(CANDIDATE_BY_ID + " AND (e.position_id IS NULL OR e.position_id &lt;&gt; p.id) GROUP BY e.id ORDER BY e.id</script>")
    List<CandidateSourceVO> candidatesByPositionId(@Param("positionId") Long positionId);

    @Select(CANDIDATE_BY_NAME + " AND (e.position IS NULL OR e.position &lt;&gt; p.position_name) GROUP BY e.id ORDER BY e.id</script>")
    List<CandidateSourceVO> candidatesByPositionName(@Param("positionId") Long positionId);

    @Select("SELECT s.skill_name FROM pos_skill_require req JOIN skill s ON s.id = req.skill_id "
            + "LEFT JOIN emp_skill own ON own.employee_id = #{employeeId} AND own.skill_id = req.skill_id "
            + "WHERE req.position_id = #{positionId} AND own.id IS NULL ORDER BY s.skill_name")
    List<String> missingSkills(@Param("positionId") Long positionId, @Param("employeeId") Long employeeId);

    @Select("SELECT req.skill_id, s.skill_name, req.requirement_type, req.required_level, "
            + "(SELECT COUNT(*) FROM emp_employee WHERE status = '在职') AS required_count, "
            + "COUNT(DISTINCT CASE WHEN e.status = '在职' THEN es.employee_id END) AS qualified_count "
            + "FROM pos_skill_require req JOIN skill s ON s.id = req.skill_id "
            + "LEFT JOIN emp_skill es ON es.skill_id = req.skill_id LEFT JOIN emp_employee e ON e.id = es.employee_id "
            + "WHERE req.position_id = #{positionId} GROUP BY req.skill_id, s.skill_name, req.requirement_type, req.required_level "
            + "ORDER BY req.requirement_type, s.skill_name")
    List<SkillCoverageVO> skillCoverage(@Param("positionId") Long positionId);

    @Select("SELECT COUNT(*) FROM emp_employee WHERE status = '在职' AND position_id = #{positionId}")
    int incumbentCountByPositionId(@Param("positionId") Long positionId);

    @Select("SELECT COUNT(*) FROM emp_employee e JOIN position p ON p.position_name = e.position "
            + "WHERE e.status = '在职' AND p.id = #{positionId}")
    int incumbentCountByPositionName(@Param("positionId") Long positionId);

    @Select("SELECT COUNT(DISTINCT e.id) FROM emp_employee e JOIN resign_warning_record r ON r.employee_id = e.id "
            + "WHERE r.warning_level = '高' AND e.status = '在职' AND e.position_id = #{positionId}")
    int highRiskByPositionId(@Param("positionId") Long positionId);

    @Select("SELECT COUNT(DISTINCT e.id) FROM emp_employee e JOIN position p ON p.position_name = e.position "
            + "JOIN resign_warning_record r ON r.employee_id = e.id "
            + "WHERE r.warning_level = '高' AND e.status = '在职' AND p.id = #{positionId}")
    int highRiskByPositionName(@Param("positionId") Long positionId);

    @Select(RISK_BY_ID + RISK_FILTER + RISK_ORDER)
    List<RiskEmployeeVO> riskEmployeesByPositionId(@Param("warningLevel") String warningLevel,
            @Param("handleStatus") String handleStatus, @Param("department") String department,
            @Param("keyword") String keyword, @Param("offset") long offset, @Param("pageSize") long pageSize);

    @Select(RISK_BY_NAME + RISK_FILTER + RISK_ORDER)
    List<RiskEmployeeVO> riskEmployeesByPositionName(@Param("warningLevel") String warningLevel,
            @Param("handleStatus") String handleStatus, @Param("department") String department,
            @Param("keyword") String keyword, @Param("offset") long offset, @Param("pageSize") long pageSize);

    @Select("<script>SELECT COUNT(*) FROM resign_warning_record r JOIN emp_employee e ON e.id = r.employee_id WHERE 1=1 "
            + RISK_FILTER + "</script>")
    long riskEmployeeCount(@Param("warningLevel") String warningLevel, @Param("handleStatus") String handleStatus,
            @Param("department") String department, @Param("keyword") String keyword);

    @Select("SELECT r.id, r.employee_id, e.emp_no, e.name AS employee_name, e.department, p.position_name AS current_position, "
            + RISK_COLUMNS + " FROM resign_warning_record r JOIN emp_employee e ON e.id = r.employee_id "
            + "LEFT JOIN position p ON p.id = e.position_id WHERE r.id = #{id}")
    RiskEmployeeVO riskDetailByPositionId(@Param("id") Long id);

    @Select("SELECT r.id, r.employee_id, e.emp_no, e.name AS employee_name, e.department, e.position AS current_position, "
            + RISK_COLUMNS + " FROM resign_warning_record r JOIN emp_employee e ON e.id = r.employee_id WHERE r.id = #{id}")
    RiskEmployeeVO riskDetailByPositionName(@Param("id") Long id);

    String CANDIDATE_SCORE_COLUMNS = "e.work_years, "
            + "(SELECT ep.score FROM emp_performance ep WHERE ep.employee_id=e.id ORDER BY ep.perf_year DESC,ep.id DESC LIMIT 1) AS performance_score, "
            + "(SELECT po.total_score FROM emp_potential po WHERE po.employee_id=e.id ORDER BY po.updated_at DESC,po.id DESC LIMIT 1) AS potential_score, "
            + "COUNT(DISTINCT own.skill_id) AS matched_skill_count, COUNT(DISTINCT req.skill_id) AS required_skill_count ";
    String CANDIDATE_FROM = "FROM position p CROSS JOIN emp_employee e LEFT JOIN pos_skill_require req ON req.position_id=p.id "
            + "LEFT JOIN emp_skill own ON own.employee_id=e.id AND own.skill_id=req.skill_id ";
    String CANDIDATE_BY_ID = "<script>SELECT e.id AS employee_id,e.emp_no,e.name AS employee_name,e.department,"
            + "current_pos.position_name AS current_position," + CANDIDATE_SCORE_COLUMNS + CANDIDATE_FROM
            + "LEFT JOIN position current_pos ON current_pos.id=e.position_id WHERE p.id=#{positionId} AND e.status='在职'";
    String CANDIDATE_BY_NAME = "<script>SELECT e.id AS employee_id,e.emp_no,e.name AS employee_name,e.department,"
            + "e.position AS current_position," + CANDIDATE_SCORE_COLUMNS + CANDIDATE_FROM
            + "WHERE p.id=#{positionId} AND e.status='在职'";
    String RISK_COLUMNS = "r.risk_score,r.warning_level,r.warning_time,r.handle_status,r.handler,r.handle_remark,r.handled_at";
    String RISK_BY_ID = "<script>SELECT r.id,r.employee_id,e.emp_no,e.name AS employee_name,e.department,p.position_name AS current_position,"
            + RISK_COLUMNS + " FROM resign_warning_record r JOIN emp_employee e ON e.id=r.employee_id LEFT JOIN position p ON p.id=e.position_id WHERE 1=1 ";
    String RISK_BY_NAME = "<script>SELECT r.id,r.employee_id,e.emp_no,e.name AS employee_name,e.department,e.position AS current_position,"
            + RISK_COLUMNS + " FROM resign_warning_record r JOIN emp_employee e ON e.id=r.employee_id WHERE 1=1 ";
    String RISK_FILTER = "<if test='warningLevel != null and warningLevel != \"\"'> AND r.warning_level=#{warningLevel}</if> "
            + "<if test='handleStatus != null and handleStatus != \"\"'> AND r.handle_status=#{handleStatus}</if> "
            + "<if test='department != null and department != \"\"'> AND e.department=#{department}</if> "
            + "<if test='keyword != null and keyword != \"\"'> AND (e.name LIKE CONCAT('%',#{keyword},'%') OR e.emp_no LIKE CONCAT('%',#{keyword},'%'))</if> ";
    String RISK_ORDER = "ORDER BY r.risk_score DESC,r.warning_time DESC LIMIT #{offset},#{pageSize}</script>";
}
