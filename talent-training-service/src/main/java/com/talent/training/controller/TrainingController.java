package com.talent.training.controller;

import com.talent.common.result.Result;
import com.talent.common.result.PageResult;
import com.talent.training.dto.CourseSaveDTO;
import com.talent.training.dto.CourseCategorySaveDTO;
import com.talent.training.dto.LearningPathSaveDTO;
import com.talent.training.dto.TrainingRecordSaveDTO;
import com.talent.training.service.TrainingService;
import com.talent.training.vo.CourseItemVO;
import com.talent.training.vo.CourseCategoryVO;
import com.talent.training.vo.CourseImportResultVO;
import com.talent.training.vo.LearningPathVO;
import com.talent.training.vo.TrainingEffectVO;
import com.talent.training.vo.TrainingPlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模块三：智能培训推荐。
 *
 * <p>推荐逻辑：拿员工所在岗位的「必备培训」减去他「已完成的培训」，剩下的就是待补课程。
 *
 * <p>直连：http://127.0.0.1:8083/api/training/paths?employeeId=1
 */
@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @GetMapping("/categories")
    public Result<List<CourseCategoryVO>> categories() {
        return Result.success(trainingService.categories());
    }

    @PostMapping("/categories")
    public Result<Long> createCategory(@RequestBody CourseCategorySaveDTO dto) {
        return Result.success("新增课程分类成功", trainingService.createCategory(dto));
    }

    @PutMapping("/categories/{id}")
    public Result<Void> updateCategory(@PathVariable("id") Long id, @RequestBody CourseCategorySaveDTO dto) {
        trainingService.updateCategory(id, dto);
        return Result.success("修改课程分类成功", null);
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable("id") Long id) {
        trainingService.deleteCategory(id);
        return Result.success("删除课程分类成功", null);
    }

    /** 学习计划：岗位必修里学完了哪些、还差哪些 */
    @GetMapping("/paths")
    public Result<TrainingPlanVO> paths(@RequestParam("employeeId") Long employeeId,
            @RequestParam(value = "developmentGoal", required = false) String developmentGoal) {
        return Result.success(trainingService.plan(employeeId, developmentGoal));
    }

    /** 这个员工已完成的培训 */
    @GetMapping("/records")
    public Result<List<CourseItemVO>> records(@RequestParam("employeeId") Long employeeId) {
        return Result.success(trainingService.completed(employeeId));
    }

    /** 课程库：支持名称模糊查询、类型筛选和分页。 */
    @GetMapping("/courses")
    public Result<PageResult<CourseItemVO>> courses(@RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                                     @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
                                                     @RequestParam(value = "keyword", required = false) String keyword,
                                                     @RequestParam(value = "courseType", required = false) String courseType,
                                                     @RequestParam(value = "categoryId", required = false) Long categoryId) {
        return Result.success(trainingService.coursePage(pageNum, pageSize, keyword, courseType, categoryId));
    }

    @PostMapping("/courses")
    public Result<Long> createCourse(@RequestBody CourseSaveDTO dto) {
        return Result.success("新增课程成功", trainingService.createCourse(dto));
    }

    @PostMapping("/courses/import")
    public Result<CourseImportResultVO> importCourses(@RequestPart("file") MultipartFile file) throws Exception {
        if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024)
            throw new com.talent.common.exception.BusinessException(com.talent.common.result.ResultCode.VALIDATE_FAILED.getCode(), "请选择不超过 2 MB 的 UTF-8 CSV 文件");
        return Result.success("课程导入完成", trainingService.importCourses(new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8)));
    }

    @GetMapping(value = "/courses/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCourses(@RequestParam(value = "keyword", required = false) String keyword,
                                                 @RequestParam(value = "courseType", required = false) String courseType,
                                                 @RequestParam(value = "categoryId", required = false) Long categoryId) {
        byte[] csv = trainingService.exportCourses(keyword, courseType, categoryId)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=training-courses.csv")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    @PutMapping("/courses/{id}")
    public Result<Void> updateCourse(@PathVariable("id") Long id, @RequestBody CourseSaveDTO dto) {
        trainingService.updateCourse(id, dto);
        return Result.success("修改课程成功", null);
    }

    @DeleteMapping("/courses/{id}")
    public Result<Void> deleteCourse(@PathVariable("id") Long id) {
        trainingService.deleteCourse(id);
        return Result.success("删除课程成功", null);
    }

    /** 可复用的学习路径模板列表；详情接口会包含分阶段任务。 */
    @GetMapping("/path-templates")
    public Result<List<LearningPathVO>> pathTemplates() {
        return Result.success(trainingService.learningPaths());
    }

    @GetMapping("/path-templates/{id}")
    public Result<LearningPathVO> pathTemplateDetail(@PathVariable("id") Long id) {
        return Result.success(trainingService.learningPathDetail(id));
    }

    @PostMapping("/path-templates")
    public Result<Long> createPathTemplate(@RequestBody LearningPathSaveDTO dto) {
        return Result.success("新增学习路径成功", trainingService.createLearningPath(dto));
    }

    @PutMapping("/path-templates/{id}")
    public Result<Void> updatePathTemplate(@PathVariable("id") Long id, @RequestBody LearningPathSaveDTO dto) {
        trainingService.updateLearningPath(id, dto);
        return Result.success("修改学习路径成功", null);
    }

    @DeleteMapping("/path-templates/{id}")
    public Result<Void> deletePathTemplate(@PathVariable("id") Long id) {
        trainingService.deleteLearningPath(id);
        return Result.success("删除学习路径成功", null);
    }

    /** 登记员工课程完成后的参与率、通过率和能力提升分值。 */
    @PostMapping("/records")
    public Result<Long> createRecord(@RequestBody TrainingRecordSaveDTO dto) {
        return Result.success("新增培训记录成功", trainingService.createRecord(dto));
    }

    @DeleteMapping("/records/{id}")
    public Result<Void> deleteRecord(@PathVariable("id") Long id) {
        trainingService.deleteRecord(id);
        return Result.success("删除培训记录成功", null);
    }

    /** 按课程汇总培训参与、考核通过和能力提升的量化效果。 */
    @GetMapping("/effects")
    public Result<List<TrainingEffectVO>> effects() {
        return Result.success(trainingService.effects());
    }
}
