package com.talent.training.service.impl;
import com.talent.training.dto.TrainingSimulationDTO;
import com.talent.training.mapper.TrainingReportMapper;
import com.talent.training.vo.TrainingReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.*;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TrainingReportService {
    private final TrainingReportMapper mapper;
    public TrainingReportVO report(TrainingSimulationDTO input) {
        var dimensions = mapper.dimensions().stream().map(row -> new TrainingReportVO.Dimension(
                String.valueOf(row.get("dimension")), String.valueOf(row.get("name")),
                ((Number) row.get("recordCount")).longValue(), ((Number) row.get("employeeCount")).longValue(),
                decimal(row.get("attendRate")), decimal(row.get("passRate")), decimal(row.get("improveScore")))).toList();
        return new TrainingReportVO(LocalDateTime.now(),
                "历史指标按培训记录计算，空值不参与均值；员工数按维度去重。模拟使用显式输入假设，不是预测或已发生的培训结果。", dimensions,
                input == null ? null : simulate(input));
    }
    public static TrainingReportVO.Simulation simulate(TrainingSimulationDTO input) {
        long attendees = BigDecimal.valueOf(input.learners()).multiply(input.attendRate()).movePointLeft(2).setScale(0, RoundingMode.HALF_UP).longValue();
        long passed = BigDecimal.valueOf(attendees).multiply(input.passRate()).movePointLeft(2).setScale(0, RoundingMode.HALF_UP).longValue();
        return new TrainingReportVO.Simulation(input, attendees, passed,
                BigDecimal.valueOf(passed).multiply(input.improveScore()).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(passed * 100).divide(BigDecimal.valueOf(input.learners()), 2, RoundingMode.HALF_UP));
    }
    public byte[] excel(TrainingReportVO report) throws IOException {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var header = book.createCellStyle(); var font = book.createFont(); font.setBold(true); header.setFont(font);
            var sheet = book.createSheet("培训效果");
            write(sheet, 0, List.of("生成时间", report.generatedAt().toString()));
            write(sheet, 1, List.of("统计说明", report.note()));
            write(sheet, 3, List.of("维度", "名称", "培训记录数", "去重员工数", "参与率（%）", "通过率（%）", "能力提升分值"));
            sheet.getRow(3).forEach(cell -> cell.setCellStyle(header));
            int index = 4;
            for (var d : report.dimensions()) write(sheet, index++, Arrays.asList(d.dimension(), d.name(), d.recordCount(), d.employeeCount(), d.attendRate(), d.passRate(), d.improveScore()));
            for (int i = 0; i < 7; i++) sheet.setColumnWidth(i, (i == 1 ? 36 : 20) * 256);
            sheet.createFreezePane(0, 4);
            if (report.simulation() != null) {
                var sim = book.createSheet("方案模拟"); var s = report.simulation(); var a = s.assumptions();
                write(sim, 0, List.of("指标", "值"));
                write(sim, 1, List.of("计划人数（假设）", a.learners()));
                write(sim, 2, List.of("参与率（假设，%）", a.attendRate()));
                write(sim, 3, List.of("通过率（假设，%）", a.passRate()));
                write(sim, 4, List.of("通过者单人提升分（假设）", a.improveScore()));
                write(sim, 5, List.of("预计参与人数", s.estimatedAttendees()));
                write(sim, 6, List.of("预计通过人数", s.estimatedPassed()));
                write(sim, 7, List.of("预计总提升分", s.totalImprovement()));
                write(sim, 8, List.of("计划人员整体通过率（%）", s.overallPassRate()));
                write(sim, 10, List.of("口径", "人数四舍五入；提升分=预计通过人数×通过者单人提升分"));
                sim.setColumnWidth(0, 42 * 256); sim.setColumnWidth(1, 65 * 256);
            }
            book.write(out); return out.toByteArray();
        }
    }
    private static void write(Sheet sheet, int index, List<?> values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.size(); i++) {
            Cell cell = row.createCell(i); Object value = values.get(i);
            if (value instanceof Number number) cell.setCellValue(number.doubleValue());
            else cell.setCellValue(value == null ? "" : value.toString());
        }
    }
    private static BigDecimal decimal(Object value) { return value == null ? null : new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP); }
}
