package com.talent.training.service.impl;

import com.talent.common.exception.BusinessException;
import com.talent.training.dto.*;
import com.talent.training.entity.*;
import com.talent.training.mapper.*;
import com.talent.training.vo.*;
import org.junit.jupiter.api.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TrainingManagementTest {
    @Test void csvSupportsQuotedCommasNewlinesAndEscapedQuotes() {
        var rows = TrainingServiceImpl.parseCsv("\"数据,分析\",专业,技能,分析师,初级,8\r\n\"跨\n行\"\"课程\",专业,技能,分析师,中级,16");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsExactly("数据,分析", "专业", "技能", "分析师", "初级", "8");
        assertThat(rows.get(1).get(0)).isEqualTo("跨\n行\"课程");
        assertThatThrownBy(() -> TrainingServiceImpl.parseCsv("\"未闭合")).isInstanceOf(BusinessException.class);
    }
    private TrainingAlgorithm algorithm() {
        TrainingAlgorithm value = new TrainingAlgorithm(); value.setName("均衡"); value.setEnabled(true);
        value.setSkillWeight(new BigDecimal("60")); value.setPositionWeight(new BigDecimal("25"));
        value.setGoalWeight(new BigDecimal("15")); value.setMatchThreshold(new BigDecimal("40")); return value;
    }
    @Test void weightsAndThresholdChangeScores() {
        TrainingAlgorithm value = algorithm();
        assertThat(TrainingMatchScorer.score(value, 1, 0, 0)).isEqualByComparingTo("60");
        value.setSkillWeight(new BigDecimal("20")); value.setPositionWeight(new BigDecimal("65"));
        assertThat(TrainingMatchScorer.score(value, 1, 0, 0)).isLessThan(value.getMatchThreshold());
        assertThat(TrainingMatchScorer.score(value, 0, 1, 1)).isEqualByComparingTo("80");
    }
    @Test void tokensAndMissingEvidenceDoNotCreateFalseMatches() {
        assertThat(TrainingMatchScorer.tokens("数据分析，团队管理;Java SQL")).containsExactly("数据分析", "团队管理", "java", "sql");
        assertThat(TrainingMatchScorer.coverage("数据分析,团队管理", List.of("数据分析"))).isEqualTo(0.5);
        assertThat(TrainingMatchScorer.coverage("经理", Arrays.asList(null, "", " "))).isZero();
    }
    @Test void invalidWeightSumAndPrecisionAreRejected() {
        TrainingAlgorithmSaveDTO dto = new TrainingAlgorithmSaveDTO(); dto.setName("测试");
        dto.setSkillWeight(new BigDecimal("60")); dto.setPositionWeight(new BigDecimal("25")); dto.setGoalWeight(new BigDecimal("15")); dto.setMatchThreshold(new BigDecimal("40"));
        assertThatCode(() -> TrainingAlgorithmService.validate(dto)).doesNotThrowAnyException();
        dto.setGoalWeight(new BigDecimal("20"));
        assertThatThrownBy(() -> TrainingAlgorithmService.validate(dto)).isInstanceOf(BusinessException.class);
        dto.setGoalWeight(new BigDecimal("15")); dto.setMatchThreshold(new BigDecimal("40.001"));
        assertThatThrownBy(() -> TrainingAlgorithmService.validate(dto)).isInstanceOf(BusinessException.class);
    }
    @Test void simulationIsDeterministicAndUsesDeclaredRounding() {
        var input = new TrainingSimulationDTO(50, new BigDecimal("85"), new BigDecimal("80"), new BigDecimal("8"));
        var result = TrainingReportService.simulate(input);
        assertThat(result.estimatedAttendees()).isEqualTo(43); assertThat(result.estimatedPassed()).isEqualTo(34);
        assertThat(result.totalImprovement()).isEqualByComparingTo("272");
        assertThat(result.overallPassRate()).isEqualByComparingTo("68");
        var zero = TrainingReportService.simulate(new TrainingSimulationDTO(50, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        assertThat(zero.estimatedPassed()).isZero();
    }
    @Test void excelContainsNumericCellsAndSimulationSheet() throws Exception {
        TrainingReportService service = new TrainingReportService(mock(TrainingReportMapper.class));
        var input = new TrainingSimulationDTO(50, new BigDecimal("85"), new BigDecimal("80"), new BigDecimal("8"));
        var report = new TrainingReportVO(LocalDateTime.now(), "测试统计说明", List.of(new TrainingReportVO.Dimension("课程", "数据分析", 2, 1, new BigDecimal("80"), null, new BigDecimal("8"))), TrainingReportService.simulate(input));
        byte[] result = service.excel(report);
        try (var book = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(book.getNumberOfSheets()).isEqualTo(2);
            assertThat(book.getSheetAt(0).getRow(4).getCell(1).getStringCellValue()).isEqualTo("数据分析");
            assertThat(book.getSheetAt(0).getRow(4).getCell(2).getNumericCellValue()).isEqualTo(2);
            assertThat(book.getSheetAt(1).getRow(7).getCell(1).getNumericCellValue()).isEqualTo(272);
        }
    }
    @Test void pdfContainsMultiplePagesAndValidXrefOffsets() throws Exception {
        List<TrainingReportVO.Dimension> dimensions = new ArrayList<>();
        for (int i = 0; i < 55; i++) dimensions.add(new TrainingReportVO.Dimension("课程", "中文培训课程" + i, 2, 1, BigDecimal.TEN, null, null));
        byte[] pdf = new TrainingPdfExporter().export(new TrainingReportVO(LocalDateTime.now(), "测试分页", dimensions, null));
        String raw = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(raw).startsWith("%PDF-1.4").contains("/Count 3").endsWith("%%EOF");
        int marker = raw.lastIndexOf("startxref\n");
        int xref = Integer.parseInt(raw.substring(marker + 10, raw.indexOf('\n', marker + 10)));
        assertThat(raw.substring(xref)).startsWith("xref");
        String[] rows = raw.substring(xref).split("\n");
        int count = Integer.parseInt(rows[1].split(" ")[1]);
        for (int i = 1; i < count; i++) {
            int offset = Integer.parseInt(rows[i + 2].substring(0, 10));
            assertThat(raw.substring(offset)).startsWith(i + " 0 obj");
        }
    }
    @Test void requiredCoursesSurviveThresholdAndCompletedCoursesAreExcluded() {
        CourseMapper courses = mock(CourseMapper.class); TrainingQueryMapper query = mock(TrainingQueryMapper.class);
        TrainingAlgorithmService algorithms = mock(TrainingAlgorithmService.class); TrainingAlgorithm config = algorithm(); config.setMatchThreshold(new BigDecimal("100")); when(algorithms.active()).thenReturn(config);
        LearningPathMapper paths = mock(LearningPathMapper.class); when(paths.selectList(any())).thenReturn(List.of());
        EmployeeTrainingProfileVO profile = new EmployeeTrainingProfileVO(); profile.setPositionName("经理"); when(query.employeeProfile(1L)).thenReturn(profile);
        when(query.skillGaps(1L)).thenReturn(List.of("Java"));
        CourseItemVO completed = new CourseItemVO(); completed.setId(2L); when(query.completedCourses(1L)).thenReturn(List.of(completed));
        CourseItemVO required = new CourseItemVO(); required.setId(1L); when(query.requiredButIncompleteCourses(1L)).thenReturn(List.of(required));
        Course a = new Course(); a.setId(1L); a.setCourseName("岗位必修"); Course b = new Course(); b.setId(2L); b.setCourseName("Java"); Course c = new Course(); c.setId(3L); c.setCourseName("Java 入门"); when(courses.selectList(any())).thenReturn(List.of(a,b,c));
        var service = new TrainingServiceImpl(algorithms, courses, mock(CourseCategoryMapper.class), paths, mock(PathTaskMapper.class), mock(TrainingRecordMapper.class), query);
        assertThat(service.plan(1L).getRecommendedCourses()).extracting(CourseItemVO::getId).containsExactly(1L);
    }
    @Test void templateImportValidatesWholeBatchBeforeWriting() {
        LearningPathMapper paths = mock(LearningPathMapper.class);
        var service = new TrainingServiceImpl(mock(TrainingAlgorithmService.class), mock(CourseMapper.class), mock(CourseCategoryMapper.class), paths, mock(PathTaskMapper.class), mock(TrainingRecordMapper.class), mock(TrainingQueryMapper.class));
        LearningPathSaveDTO good = new LearningPathSaveDTO(); good.setPathName("有效模板");
        LearningPathSaveDTO.PathTaskDTO task = new LearningPathSaveDTO.PathTaskDTO(); task.setStage(1); task.setTaskName("基础训练"); good.setTasks(List.of(task));
        LearningPathSaveDTO bad = new LearningPathSaveDTO(); bad.setPathName("缺少任务");
        assertThatThrownBy(() -> service.importPaths(List.of(good,bad))).isInstanceOf(BusinessException.class);
        verify(paths, never()).insert(any(LearningPath.class));
    }
}
