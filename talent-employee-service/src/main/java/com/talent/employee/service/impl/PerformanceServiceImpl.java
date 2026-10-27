package com.talent.employee.service.impl;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talent.common.exception.BusinessException;
import com.talent.common.result.ResultCode;
import com.talent.employee.dto.PerformanceSaveDTO;
import com.talent.employee.entity.Performance;
import com.talent.employee.excel.PerformanceExcelRow;
import com.talent.employee.mapper.PerformanceMapper;
import com.talent.employee.service.PerformanceService;
import com.talent.employee.vo.PerformanceCompareVO;
import com.talent.employee.vo.PerformanceImportResultVO;
import com.talent.employee.vo.PerformanceTrendVO;
import com.talent.employee.vo.PerformanceVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 历年绩效实现。
 *
 * <p>一个员工每年一条（emp_performance 有 employee_id + perf_year 唯一键），
 * 录入时按「员工 + 年份」做 upsert，不会重复。
 */
@Service
@RequiredArgsConstructor
public class PerformanceServiceImpl implements PerformanceService {

    private final PerformanceMapper performanceMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<PerformanceVO> history(Long employeeId) {
        List<PerformanceVO> list = performanceMapper.selectHistory(employeeId);
        list.forEach(this::parseDimensions);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PerformanceVO save(PerformanceSaveDTO dto) {
        if (performanceMapper.countEmployee(dto.getEmployeeId()) == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "员工不存在：" + dto.getEmployeeId());
        }
        upsert(dto);
        PerformanceVO vo = performanceMapper.selectEmployeeYear(dto.getEmployeeId(), dto.getPerfYear());
        parseDimensions(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSave(List<PerformanceSaveDTO> records) {
        for (PerformanceSaveDTO record : records) {
            if (performanceMapper.countEmployee(record.getEmployeeId()) == 0) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                        "员工不存在：" + record.getEmployeeId());
            }
        }
        records.forEach(this::upsert);
        return records.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PerformanceSaveDTO dto) {
        Performance exists = performanceMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "绩效记录不存在：" + id);
        }
        Performance entity = new Performance();
        entity.setId(id);
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setPerfYear(dto.getPerfYear());
        entity.setScore(dto.getScore());
        entity.setGrade(StringUtils.hasText(dto.getGrade()) ? dto.getGrade() : deriveGrade(dto.getScore()));
        entity.setDimensionScores(toJson(dto.getDimensions()));
        performanceMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        if (performanceMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "绩效记录不存在：" + id);
        }
        performanceMapper.deleteById(id);
    }

    @Override
    public PerformanceTrendVO trend(Long employeeId) {
        List<PerformanceVO> history = history(employeeId);
        PerformanceTrendVO vo = new PerformanceTrendVO();
        vo.setEmployeeId(employeeId);
        if (history.isEmpty()) {
            vo.setTrend("暂无数据");
            return vo;
        }
        vo.setEmpNo(history.get(0).getEmpNo());
        vo.setName(history.get(0).getName());

        Set<String> dimensionKeys = new LinkedHashSet<>();
        for (PerformanceVO item : history) {
            vo.getYears().add(item.getPerfYear());
            vo.getScores().add(item.getScore());
            vo.getGrades().add(item.getGrade());
            dimensionKeys.addAll(item.getDimensions().keySet());
        }
        vo.setDimensionKeys(new ArrayList<>(dimensionKeys));

        for (String key : dimensionKeys) {
            PerformanceTrendVO.Series series = new PerformanceTrendVO.Series();
            series.setName(key);
            for (PerformanceVO item : history) {
                series.getData().add(item.getDimensions().getOrDefault(key, BigDecimal.ZERO));
            }
            vo.getDimensionSeries().add(series);
        }

        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal max = history.get(0).getScore();
        BigDecimal min = history.get(0).getScore();
        for (PerformanceVO item : history) {
            BigDecimal score = item.getScore() == null ? BigDecimal.ZERO : item.getScore();
            sum = sum.add(score);
            max = max.max(score);
            min = min.min(score);
        }
        vo.setAverageScore(sum.divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP));
        vo.setMaxScore(max);
        vo.setMinScore(min);

        if (history.size() >= 2) {
            BigDecimal last = history.get(history.size() - 1).getScore();
            BigDecimal previous = history.get(history.size() - 2).getScore();
            BigDecimal delta = last.subtract(previous);
            vo.setYoyChange(delta);
            vo.setTrend(delta.compareTo(new BigDecimal("0.1")) > 0 ? "上升"
                    : delta.compareTo(new BigDecimal("-0.1")) < 0 ? "下降" : "平稳");
        } else {
            vo.setTrend("数据不足");
        }
        return vo;
    }

    @Override
    public PerformanceCompareVO compare(Long employeeId, Integer yearA, Integer yearB) {
        PerformanceVO a = performanceMapper.selectEmployeeYear(employeeId, yearA);
        PerformanceVO b = performanceMapper.selectEmployeeYear(employeeId, yearB);
        if (a == null || b == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                    "缺少 " + yearA + " 或 " + yearB + " 年的绩效数据");
        }
        parseDimensions(a);
        parseDimensions(b);

        PerformanceCompareVO vo = new PerformanceCompareVO();
        vo.setEmployeeId(employeeId);
        vo.setEmpNo(a.getEmpNo());
        vo.setName(a.getName());
        vo.setYearA(yearA);
        vo.setYearB(yearB);
        vo.setScoreA(a.getScore());
        vo.setScoreB(b.getScore());
        vo.setGradeA(a.getGrade());
        vo.setGradeB(b.getGrade());
        BigDecimal delta = b.getScore().subtract(a.getScore());
        vo.setDelta(delta);
        vo.setConclusion(delta.compareTo(new BigDecimal("0.1")) > 0 ? "提升"
                : delta.compareTo(new BigDecimal("-0.1")) < 0 ? "下降" : "持平");

        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(a.getDimensions().keySet());
        keys.addAll(b.getDimensions().keySet());
        for (String key : keys) {
            BigDecimal va = a.getDimensions().getOrDefault(key, BigDecimal.ZERO);
            BigDecimal vb = b.getDimensions().getOrDefault(key, BigDecimal.ZERO);
            PerformanceCompareVO.DimensionDiff diff = new PerformanceCompareVO.DimensionDiff();
            diff.setName(key);
            diff.setValueA(va);
            diff.setValueB(vb);
            diff.setDelta(vb.subtract(va));
            vo.getDimensions().add(diff);
        }
        return vo;
    }

    @Override
    public void export(Long employeeId, HttpServletResponse response) throws IOException {
        List<PerformanceExcelRow> rows = new ArrayList<>();
        for (PerformanceVO item : performanceMapper.selectForExport(employeeId)) {
            parseDimensions(item);
            PerformanceExcelRow row = new PerformanceExcelRow();
            row.setEmpNo(item.getEmpNo());
            row.setPerfYear(item.getPerfYear());
            row.setScore(item.getScore());
            row.setGrade(item.getGrade());
            row.setDimensionScores(toJson(item.getDimensions()));
            rows.add(row);
        }
        write(response, "员工历年绩效", rows);
    }

    @Override
    public void template(HttpServletResponse response) throws IOException {
        PerformanceExcelRow row = new PerformanceExcelRow();
        row.setEmpNo("E70014679");
        row.setPerfYear(2026);
        row.setScore(new BigDecimal("4.5"));
        row.setGrade("A");
        row.setDimensionScores("{\"业绩\":4.5,\"协作\":4.2,\"创新\":4.0}");
        write(response, "绩效导入模板", List.of(row));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PerformanceImportResultVO importExcel(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "请选择绩效 Excel 文件");
        }
        List<Object> raw;
        try {
            raw = EasyExcel.read(file.getInputStream()).head(PerformanceExcelRow.class).sheet().doReadSync();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(),
                    "文件解析失败，请使用导入模板：" + e.getMessage());
        }

        PerformanceImportResultVO result = new PerformanceImportResultVO();
        result.setTotal(raw.size());
        int success = 0;
        for (int i = 0; i < raw.size(); i++) {
            PerformanceExcelRow row = (PerformanceExcelRow) raw.get(i);
            int rowNo = i + 2;
            String empNo = row.getEmpNo() == null ? null : row.getEmpNo().trim();
            if (!StringUtils.hasText(empNo) || row.getPerfYear() == null || row.getScore() == null) {
                result.addError(rowNo, empNo, "工号、年份、绩效评分是必填的");
                continue;
            }
            Long employeeId = performanceMapper.selectEmployeeIdByEmpNo(empNo);
            if (employeeId == null) {
                result.addError(rowNo, empNo, "系统中没有这个工号");
                continue;
            }
            if (row.getScore().compareTo(BigDecimal.ZERO) < 0
                    || row.getScore().compareTo(new BigDecimal("5")) > 0) {
                result.addError(rowNo, empNo, "绩效评分要在 0~5 之间");
                continue;
            }
            Map<String, BigDecimal> dimensions = new LinkedHashMap<>();
            if (StringUtils.hasText(row.getDimensionScores())) {
                try {
                    dimensions = objectMapper.readValue(row.getDimensionScores(), new TypeReference<>() { });
                } catch (Exception e) {
                    result.addError(rowNo, empNo, "维度得分不是合法 JSON");
                    continue;
                }
            }
            PerformanceSaveDTO dto = new PerformanceSaveDTO();
            dto.setEmployeeId(employeeId);
            dto.setPerfYear(row.getPerfYear());
            dto.setScore(row.getScore());
            dto.setGrade(row.getGrade());
            dto.setDimensions(dimensions);
            upsert(dto);
            success++;
        }
        result.setSuccess(success);
        return result;
    }

    /* ---------------- 内部方法 ---------------- */

    private void upsert(PerformanceSaveDTO dto) {
        Performance exists = performanceMapper.selectEntity(dto.getEmployeeId(), dto.getPerfYear());
        Performance entity = new Performance();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setPerfYear(dto.getPerfYear());
        entity.setScore(dto.getScore());
        entity.setGrade(StringUtils.hasText(dto.getGrade()) ? dto.getGrade() : deriveGrade(dto.getScore()));
        entity.setDimensionScores(toJson(dto.getDimensions()));
        if (exists == null) {
            performanceMapper.insert(entity);
        } else {
            entity.setId(exists.getId());
            performanceMapper.updateById(entity);
        }
    }

    private String deriveGrade(BigDecimal score) {
        if (score.compareTo(new BigDecimal("4.5")) >= 0) return "A";
        if (score.compareTo(new BigDecimal("3.5")) >= 0) return "B";
        if (score.compareTo(new BigDecimal("2.5")) >= 0) return "C";
        return "D";
    }

    private void parseDimensions(PerformanceVO vo) {
        Map<String, BigDecimal> dimensions = new LinkedHashMap<>();
        if (StringUtils.hasText(vo.getDimensionScores())) {
            try {
                dimensions = objectMapper.readValue(vo.getDimensionScores(), new TypeReference<>() { });
            } catch (Exception ignored) {
                // 维度 JSON 不合法时按空维度处理，不影响主流程
            }
        }
        vo.setDimensions(dimensions);
    }

    private String toJson(Map<String, BigDecimal> dimensions) {
        try {
            return objectMapper.writeValueAsString(dimensions == null ? Map.of() : dimensions);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "维度得分格式不正确");
        }
    }

    private void write(HttpServletResponse response, String fileName, List<PerformanceExcelRow> rows) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String name = URLEncoder.encode(fileName + "_" + LocalDate.now(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + name + ".xlsx");
        EasyExcel.write(response.getOutputStream(), PerformanceExcelRow.class).sheet("绩效").doWrite(rows);
    }
}
