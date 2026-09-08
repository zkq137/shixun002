package com.talent.training.controller;

import com.talent.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能培训推荐 API。示例接口，实际业务在此模块内扩展。
 */
@RestController
@RequestMapping("/api/training")
public class TrainingController {

    @GetMapping("/paths")
    public Result<String> paths() {
        return Result.success("根据员工画像匹配的学习路径（示例数据）");
    }
}
