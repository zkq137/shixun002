package com.talent.training.controller;
import com.talent.common.result.Result;
import com.talent.common.result.ResultCode;
import com.talent.common.exception.BusinessException;
import com.talent.training.dto.*;
import com.talent.training.entity.TrainingAlgorithm;
import com.talent.training.vo.TrainingReportVO;
import com.talent.training.service.TrainingService;
import com.talent.training.service.impl.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingManagementController {
    private final TrainingService trainingService;
    private final TrainingAlgorithmService algorithms;
    private final TrainingReportService reports;
    private final TrainingPdfExporter pdf;
    @GetMapping("/algorithms") public Result<List<TrainingAlgorithm>> algorithms() { return Result.success(algorithms.list()); }
    @PostMapping("/algorithms") public Result<Long> create(@RequestBody TrainingAlgorithmSaveDTO dto) { return Result.success(algorithms.save(null, dto)); }
    @PutMapping("/algorithms/{id}") public Result<Long> update(@PathVariable("id") Long id, @RequestBody TrainingAlgorithmSaveDTO dto) { return Result.success(algorithms.save(id, dto)); }
    @DeleteMapping("/algorithms/{id}") public Result<Void> delete(@PathVariable("id") Long id) { algorithms.delete(id); return Result.success(null); }
    @PostMapping("/path-templates/import") public Result<Integer> importPaths(@RequestBody List<LearningPathSaveDTO> templates) { return Result.success(trainingService.importPaths(templates)); }
    @GetMapping("/path-templates/export") public ResponseEntity<?> exportPaths() {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=learning-path-templates.json").contentType(MediaType.APPLICATION_JSON).body(trainingService.learningPaths());
    }
    @GetMapping("/reports") public Result<TrainingReportVO> report() { return Result.success(reports.report(null)); }
    @PostMapping("/reports/simulate") public Result<TrainingReportVO> simulate(@Valid @RequestBody TrainingSimulationDTO input) { return Result.success(reports.report(input)); }
    @PostMapping("/reports/export") public ResponseEntity<byte[]> export(@RequestParam("format") String format,
            @Valid @RequestBody(required = false) TrainingSimulationDTO input) throws Exception {
        if (!List.of("xlsx", "pdf").contains(format)) throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "仅支持 xlsx 或 pdf");
        var report = reports.report(input);
        byte[] data = format.equals("xlsx") ? reports.excel(report) : pdf.export(report);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=training-report." + format)
                .contentType(MediaType.parseMediaType(format.equals("pdf") ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(data);
    }
}
