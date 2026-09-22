package com.talent.employee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.talent.common.result.PageResult;
import com.talent.common.vo.NameValueVO;
import com.talent.employee.dto.EmployeeBatchUpdateDTO;
import com.talent.employee.dto.EmployeeQuery;
import com.talent.employee.dto.EmployeeSaveDTO;
import com.talent.employee.entity.Employee;
import com.talent.employee.vo.BatchResultVO;
import com.talent.employee.vo.EmployeeDetailVO;
import com.talent.employee.vo.EmployeeVO;
import com.talent.employee.vo.PositionOptionVO;
import com.talent.employee.vo.SkillVO;

import java.util.List;

/**
 * 员工档案业务接口。
 */
public interface EmployeeService extends IService<Employee> {

    /** 分页查询 */
    PageResult<EmployeeVO> pageQuery(EmployeeQuery query);

    /** 取前 N 条，给快速联调用 */
    List<EmployeeVO> top(int limit);

    /** 详情：基本信息 + 薪酬 + 技能 + 培训 */
    EmployeeDetailVO detail(Long id);

    /** 新增，返回新员工ID */
    Long create(EmployeeSaveDTO dto);

    /** 修改 */
    void update(Long id, EmployeeSaveDTO dto);

    /** 删除（薪酬/绩效/技能等关联记录由外键级联删除） */
    void delete(Long id);

    /** 按部门统计人数 */
    List<NameValueVO> departmentStats();

    /** 岗位下拉列表 */
    List<PositionOptionVO> positionOptions();

    /** 部门下拉列表 */
    List<String> departments();

    /** 批量删除 */
    BatchResultVO batchDelete(List<Long> ids);

    /** 批量修改（部门/状态/办公方式/职级） */
    BatchResultVO batchUpdate(EmployeeBatchUpdateDTO dto);

    /** 某员工会的技能 */
    List<SkillVO> employeeSkills(Long employeeId);

    /** 覆盖式设置某员工的技能 */
    void replaceSkills(Long employeeId, List<Long> skillIds);
}
