package com.talent.training.controller;

import com.talent.common.result.Result;
import com.talent.training.service.TrainingService;
import com.talent.training.vo.CourseItemVO;
import com.talent.training.vo.TrainingPlanVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}
