package com.talent.employee.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.PageResult;
import com.talent.common.result.ResultCode;
import com.talent.common.vo.NameValueVO;
import com.talent.employee.dto.EmployeeBatchUpdateDTO;
import com.talent.employee.dto.EmployeeQuery;
import com.talent.employee.dto.EmployeeSaveDTO;
import com.talent.employee.entity.Employee;
import com.talent.employee.mapper.EmployeeMapper;
import com.talent.employee.service.EmployeeService;
import com.talent.employee.vo.BatchResultVO;
import com.talent.employee.vo.EmployeeDetailVO;
import com.talent.employee.vo.EmployeeVO;
import com.talent.employee.vo.PositionOptionVO;
import com.talent.employee.vo.SkillVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 员工档案业务实现。
 */
@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    /**
     * 数据集里的绩效没有年份，统一按这一年查。
     * 要换年度时在 application.yml 里加 talent.perf-year，不用改代码。
     */
    @Value("${talent.perf-year:2026}")
    private int perfYear;

    @Override
    public PageResult<EmployeeVO> pageQuery(EmployeeQuery query) {
        Page<EmployeeVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<EmployeeVO> result = baseMapper.selectEmployeePage(page, query, perfYear);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public List<EmployeeVO> top(int limit) {
        EmployeeQuery query = new EmployeeQuery();
        query.setPageSize(Math.max(1, Math.min(limit, 200)));
        return pageQuery(query).getRecords();
    }

    @Override
    public EmployeeDetailVO detail(Long id) {
        EmployeeDetailVO vo = baseMapper.selectEmployeeDetail(id, perfYear);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "员工不存在：" + id);
        }
        vo.setSkills(baseMapper.selectSkillNames(id));
        vo.setCourses(baseMapper.selectCourseNames(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(EmployeeSaveDTO dto) {
        checkPosition(dto.getPositionId());
        if (baseMapper.countByEmpNo(dto.getEmpNo(), null) > 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "工号已存在：" + dto.getEmpNo());
        }
        Employee entity = toEntity(dto);
        entity.setId(null);
        save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, EmployeeSaveDTO dto) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "员工不存在：" + id);
        }
        checkPosition(dto.getPositionId());
        if (baseMapper.countByEmpNo(dto.getEmpNo(), id) > 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "工号已被别人占用：" + dto.getEmpNo());
        }
        Employee entity = toEntity(dto);
        entity.setId(id);
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "员工不存在：" + id);
        }
        removeById(id);
    }

    @Override
    public List<NameValueVO> departmentStats() {
        return baseMapper.countByDepartment();
    }

    @Override
    public List<PositionOptionVO> positionOptions() {
        return baseMapper.selectPositionOptions();
    }

    @Override
    public List<String> departments() {
        return baseMapper.selectDepartments();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchResultVO batchDelete(List<Long> ids) {
        List<Long> wanted = ids.stream().distinct().toList();
        List<Employee> exists = listByIds(wanted);
        if (!exists.isEmpty()) {
            removeByIds(exists.stream().map(Employee::getId).toList());
        }
        return BatchResultVO.of(wanted.size(), exists.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchResultVO batchUpdate(EmployeeBatchUpdateDTO dto) {
        boolean hasField = StringUtils.hasText(dto.getDepartment())
                || StringUtils.hasText(dto.getStatus())
                || StringUtils.hasText(dto.getWorkMode())
                || StringUtils.hasText(dto.getLevelTier())
                || StringUtils.hasText(dto.getJobRank());
        if (!hasField) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "至少要选一个要修改的字段");
        }

        List<Long> wanted = dto.getIds().stream().distinct().toList();
        long exists = count(Wrappers.<Employee>lambdaQuery().in(Employee::getId, wanted));
        if (exists == 0) {
            return BatchResultVO.of(wanted.size(), 0);
        }

        var update = lambdaUpdate().in(Employee::getId, wanted);
        update.set(StringUtils.hasText(dto.getDepartment()), Employee::getDepartment, dto.getDepartment());
        update.set(StringUtils.hasText(dto.getStatus()), Employee::getStatus, dto.getStatus());
        update.set(StringUtils.hasText(dto.getWorkMode()), Employee::getWorkMode, dto.getWorkMode());
        update.set(StringUtils.hasText(dto.getLevelTier()), Employee::getLevelTier, dto.getLevelTier());
        update.set(StringUtils.hasText(dto.getJobRank()), Employee::getJobRank, dto.getJobRank());
        update.update();

        return BatchResultVO.of(wanted.size(), (int) exists);
    }

    @Override
    public List<SkillVO> employeeSkills(Long employeeId) {
        if (getById(employeeId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "员工不存在：" + employeeId);
        }
        return baseMapper.selectEmployeeSkills(employeeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceSkills(Long employeeId, List<Long> skillIds) {
        if (getById(employeeId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "员工不存在：" + employeeId);
        }
        List<Long> wanted = skillIds == null ? List.of() : skillIds.stream().distinct().toList();
        if (!wanted.isEmpty()) {
            List<Long> valid = baseMapper.selectValidSkillIds(wanted);
            if (valid.size() != wanted.size()) {
                List<Long> bad = wanted.stream().filter(id -> !valid.contains(id)).toList();
                throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                        "这些技能不存在或不是具体技能（分类不能选）：" + bad);
            }
        }
        baseMapper.deleteEmployeeSkills(employeeId);
        if (!wanted.isEmpty()) {
            baseMapper.insertEmployeeSkills(employeeId, wanted);
        }
    }

    private void checkPosition(Long positionId) {
        if (positionId == null || baseMapper.countPosition(positionId) == 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "岗位不存在：" + positionId);
        }
    }

    private Employee toEntity(EmployeeSaveDTO dto) {
        Employee entity = new Employee();
        entity.setEmpNo(dto.getEmpNo().trim());
        entity.setName(dto.getName().trim());
        entity.setGender(dto.getGender());
        entity.setAge(dto.getAge());
        entity.setDepartment(dto.getDepartment());
        entity.setPositionId(dto.getPositionId());
        entity.setLevelTier(dto.getLevelTier());
        entity.setJobRank(dto.getJobRank());
        entity.setManagerEmpNo(StringUtils.hasText(dto.getManagerEmpNo()) ? dto.getManagerEmpNo().trim() : null);
        entity.setWorkMode(dto.getWorkMode());
        entity.setTenureYears(dto.getTenureYears());
        entity.setEducation(dto.getEducation());
        entity.setPhone(dto.getPhone());
        entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "在职");
        return entity;
    }
}
