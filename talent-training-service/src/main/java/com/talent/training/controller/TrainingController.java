package com.talent.training.controller;

import com.talent.common.result.Result;
import com.talent.common.result.PageResult;
import com.talent.training.dto.CourseSaveDTO;
import com.talent.training.dto.LearningPathSaveDTO;
import com.talent.training.dto.TrainingRecordSaveDTO;
import com.talent.training.service.TrainingService;
import com.talent.training.vo.CourseItemVO;
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

    /** 学习计划：岗位必修里学完了哪些、还差哪些 */
    @GetMapping("/paths")
    public Result<TrainingPlanVO> paths(@RequestParam("employeeId") Long employeeId) {
        return Result.success(trainingService.plan(employeeId));
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
                                                     @RequestParam(value = "courseType", required = false) String courseType) {
        return Result.success(trainingService.coursePage(pageNum, pageSize, keyword, courseType));
    }

    @PostMapping("/courses")
    public Result<Long> createCourse(@RequestBody CourseSaveDTO dto) {
        return Result.success("新增课程成功", trainingService.createCourse(dto));
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
