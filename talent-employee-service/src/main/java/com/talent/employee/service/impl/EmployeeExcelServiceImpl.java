package com.talent.employee.service.impl;

import com.alibaba.excel.EasyExcel;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.PageResult;
import com.talent.common.result.ResultCode;
import com.talent.employee.dto.EmployeeQuery;
import com.talent.employee.entity.Employee;
import com.talent.employee.excel.EmployeeExcelRow;
import com.talent.employee.mapper.EmployeeMapper;
import com.talent.employee.service.EmployeeExcelService;
import com.talent.employee.service.EmployeeService;
import com.talent.employee.vo.EmployeeImportResultVO;
import com.talent.employee.vo.EmployeeSkillNameVO;
import com.talent.employee.vo.EmployeeVO;
import com.talent.employee.vo.PositionOptionVO;
import com.talent.employee.vo.SkillVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Excel 导入导出实现（EasyExcel）。
 */
@Service
@RequiredArgsConstructor
public class EmployeeExcelServiceImpl implements EmployeeExcelService {

    /** 导出一页取多少条 */
    private static final int PAGE_SIZE = 200;

    /** 导出上限，防止有人不筛选直接导出把内存打爆 */
    private static final int MAX_EXPORT = 20000;

    private final EmployeeService employeeService;
    private final EmployeeMapper employeeMapper;

    @Override
    public void export(EmployeeQuery query, HttpServletResponse response) throws IOException {
        List<EmployeeExcelRow> rows = new ArrayList<>();
        long pageNum = 1;

        while (rows.size() < MAX_EXPORT) {
            EmployeeQuery pageQuery = copyOf(query, pageNum);
            PageResult<EmployeeVO> page = employeeService.pageQuery(pageQuery);
            List<EmployeeVO> records = page.getRecords();
            if (records.isEmpty()) {
                break;
            }

            List<Long> ids = records.stream().map(EmployeeVO::getId).toList();
            Map<Long, String> skills = new HashMap<>();
            for (EmployeeSkillNameVO item : employeeMapper.selectSkillNamesByEmployeeIds(ids)) {
                skills.merge(item.getEmployeeId(), item.getSkillName(), (a, b) -> a + "、" + b);
            }

            for (EmployeeVO vo : records) {
                rows.add(toRow(vo, skills.get(vo.getId())));
            }
            if (rows.size() >= page.getTotal()) {
                break;
            }
            pageNum++;
        }

        write(response, "员工档案", rows);
    }

    @Override
    public void template(HttpServletResponse response) throws IOException {
        EmployeeExcelRow sample = new EmployeeExcelRow();
        sample.setEmpNo("E90000001");
        sample.setName("张三");
        sample.setGender("男");
        sample.setAge(28);
        sample.setDepartment("研发中心");
        sample.setPositionName("软件工程师");
        sample.setLevelTier("员工层");
        sample.setJobRank("中级");
        sample.setWorkMode("现场办公");
        sample.setTenureYears(new BigDecimal("1.5"));
        sample.setStatus("在职");
        sample.setSkills("Java、Git");

        write(response, "员工导入模板", List.of(sample));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeImportResultVO importExcel(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "请先选择要导入的 Excel 文件");
        }

        List<Object> raw;
        try {
            raw = EasyExcel.read(file.getInputStream()).head(EmployeeExcelRow.class).sheet().doReadSync();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "文件解析失败，请用模板重新填写：" + e.getMessage());
        }

        EmployeeImportResultVO result = new EmployeeImportResultVO();
        result.setTotal(raw.size());

        List<Employee> toSave = new ArrayList<>();
        Map<String, List<String>> skillsByEmpNo = new LinkedHashMap<>();
        Set<String> empNosInFile = new HashSet<>();

        for (int i = 0; i < raw.size(); i++) {
            EmployeeExcelRow row = (EmployeeExcelRow) raw.get(i);
            int rowNo = i + 2; // 第 1 行是表头
            String empNo = trim(row.getEmpNo());
            String name = trim(row.getName());
            String positionName = trim(row.getPositionName());

            if (!StringUtils.hasText(empNo) || !StringUtils.hasText(name) || !StringUtils.hasText(positionName)) {
                result.addError(rowNo, empNo, "工号、姓名、岗位是必填的");
                continue;
            }
            if (!empNosInFile.add(empNo)) {
                result.addError(rowNo, empNo, "文件里这个工号重复了");
                continue;
            }
            if (employeeMapper.countByEmpNo(empNo, null) > 0) {
                result.addError(rowNo, empNo, "系统里已经有这个工号了");
                continue;
            }
            if (row.getAge() != null && (row.getAge() < 16 || row.getAge() > 100)) {
                result.addError(rowNo, empNo, "年龄要在 16~100 之间");
                continue;
            }

            PositionOptionVO position = employeeMapper.selectPositionByName(positionName, trim(row.getDepartment()));
            if (position == null) {
                result.addError(rowNo, empNo, "找不到岗位「" + positionName + "」，请照着岗位下拉里的名字填");
                continue;
            }

            Employee entity = new Employee();
            entity.setEmpNo(empNo);
            entity.setName(name);
            entity.setGender(trim(row.getGender()));
            entity.setAge(row.getAge());
            entity.setDepartment(StringUtils.hasText(row.getDepartment()) ? trim(row.getDepartment()) : position.getDepartment());
            entity.setPositionId(position.getId());
            entity.setLevelTier(StringUtils.hasText(row.getLevelTier()) ? trim(row.getLevelTier()) : position.getLevelTier());
            entity.setJobRank(trim(row.getJobRank()));
            entity.setManagerEmpNo(trim(row.getManagerEmpNo()));
            entity.setWorkMode(trim(row.getWorkMode()));
            entity.setTenureYears(row.getTenureYears());
            entity.setStatus(StringUtils.hasText(row.getStatus()) ? trim(row.getStatus()) : "在职");
            toSave.add(entity);

            if (StringUtils.hasText(row.getSkills())) {
                skillsByEmpNo.put(empNo, splitSkills(row.getSkills()));
            }
        }

        if (!toSave.isEmpty()) {
            employeeService.saveBatch(toSave);
        }
        result.setSuccess(toSave.size());

        // 技能标签：只认技能库里已有的技能，对不上的收集起来提示用户
        if (!skillsByEmpNo.isEmpty()) {
            Set<String> allNames = new LinkedHashSet<>();
            skillsByEmpNo.values().forEach(allNames::addAll);

            Map<String, Long> nameToId = new HashMap<>();
            for (SkillVO skill : employeeMapper.selectSkillsByNames(new ArrayList<>(allNames))) {
                nameToId.put(skill.getSkillName(), skill.getId());
            }
            Set<String> unknown = new LinkedHashSet<>(allNames);
            unknown.removeAll(nameToId.keySet());
            result.setUnknownSkills(unknown);

            Map<String, Long> empNoToId = new HashMap<>();
            toSave.forEach(e -> empNoToId.put(e.getEmpNo(), e.getId()));

            skillsByEmpNo.forEach((empNo, names) -> {
                List<Long> ids = names.stream().map(nameToId::get).filter(java.util.Objects::nonNull).distinct().toList();
                if (!ids.isEmpty()) {
                    employeeMapper.insertEmployeeSkills(empNoToId.get(empNo), ids);
                }
            });
        }
        return result;
    }

    private EmployeeQuery copyOf(EmployeeQuery source, long pageNum) {
        EmployeeQuery query = new EmployeeQuery();
        query.setPageNum(pageNum);
        query.setPageSize(PAGE_SIZE);
        query.setKeyword(source.getKeyword());
        query.setDepartment(source.getDepartment());
        query.setPositionId(source.getPositionId());
        query.setTalentTag(source.getTalentTag());
        query.setWarningLevel(source.getWarningLevel());
        query.setStatus(source.getStatus());
        return query;
    }

    private EmployeeExcelRow toRow(EmployeeVO vo, String skills) {
        EmployeeExcelRow row = new EmployeeExcelRow();
        row.setEmpNo(vo.getEmpNo());
        row.setName(vo.getName());
        row.setGender(vo.getGender());
        row.setAge(vo.getAge());
        row.setDepartment(vo.getDepartment());
        row.setPositionName(vo.getPositionName());
        row.setLevelTier(vo.getLevelTier());
        row.setJobRank(vo.getJobRank());
        row.setManagerEmpNo(vo.getManagerEmpNo());
        row.setWorkMode(vo.getWorkMode());
        row.setTenureYears(vo.getTenureYears());
        row.setStatus(vo.getStatus());
        row.setSkills(skills);
        row.setTalentTag(vo.getTalentTag());
        row.setPerfScore(vo.getPerfScore());
        row.setPotentialLevel(vo.getPotentialLevel());
        row.setWarningLevel(vo.getWarningLevel());
        return row;
    }

    private void write(HttpServletResponse response, String fileName, List<EmployeeExcelRow> rows) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String name = URLEncoder.encode(fileName + "_" + LocalDate.now(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + name + ".xlsx");
        EasyExcel.write(response.getOutputStream(), EmployeeExcelRow.class).sheet("员工档案").doWrite(rows);
    }

    private List<String> splitSkills(String text) {
        List<String> names = new ArrayList<>();
        for (String part : text.split("[、,，/]")) {
            String name = part.trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
