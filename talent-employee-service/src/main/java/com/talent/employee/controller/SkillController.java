package com.talent.employee.controller;

import com.talent.common.result.Result;
import com.talent.employee.dto.SkillSaveDTO;
import com.talent.employee.service.SkillService;
import com.talent.employee.vo.SkillTreeVO;
import com.talent.employee.vo.SkillVO;
import jakarta.validation.Valid;
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
 * 企业标准化技能树：分类 + 子技能的增删改查。
 *
 * <p>直连：http://127.0.0.1:8081/api/skill/tree
 * <p>走网关：http://127.0.0.1:9090/api/skill/tree
 */
@RestController
@RequestMapping("/api/skill")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    /** 整棵技能树（分类 + 子技能，带引用次数） */
    @GetMapping("/tree")
    public Result<List<SkillTreeVO>> tree() {
        return Result.success(skillService.tree());
    }

    /** 只要分类，给下拉用 */
    @GetMapping("/categories")
    public Result<List<SkillVO>> categories() {
        return Result.success(skillService.categories());
    }

    /** 某个分类下的技能；parentId 不传就是查顶级分类 */
    @GetMapping("/children")
    public Result<List<SkillVO>> children(@RequestParam(value = "parentId", required = false) Long parentId) {
        return Result.success(skillService.listByParent(parentId));
    }

    /** 按关键字搜技能 */
    @GetMapping("/search")
    public Result<List<SkillVO>> search(@RequestParam("keyword") String keyword) {
        return Result.success(skillService.search(keyword));
    }

    /** 新增分类或子技能：parentId 传 0 或者不传 = 新建分类 */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SkillSaveDTO dto) {
        return Result.success("新增成功", skillService.create(dto));
    }

    /** 修改技能名称/说明/排序/状态，也可以换分类 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody SkillSaveDTO dto) {
        skillService.update(id, dto);
        return Result.success("修改成功", null);
    }

    /** 删除技能或分类 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        skillService.delete(id);
        return Result.success("删除成功", null);
    }
}
