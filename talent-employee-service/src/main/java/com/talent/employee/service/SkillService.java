package com.talent.employee.service;

import com.talent.employee.dto.SkillSaveDTO;
import com.talent.employee.vo.SkillTreeVO;
import com.talent.employee.vo.SkillVO;

import java.util.List;

/**
 * 技能树：分类和子技能的维护。
 */
public interface SkillService {

    /** 整棵树（分类 + 子技能） */
    List<SkillTreeVO> tree();

    /** 分类列表 */
    List<SkillVO> categories();

    /** 某个分类下的技能 */
    List<SkillVO> listByParent(Long parentId);

    /** 按关键字搜技能 */
    List<SkillVO> search(String keyword);

    /** 新增：parentId 为 0/空时新增一个顶级分类 */
    Long create(SkillSaveDTO dto);

    /** 修改名称/说明/排序/状态；也支持把技能挪到别的分类下 */
    void update(Long id, SkillSaveDTO dto);

    /** 删除。分类下有子技能、或技能已被员工/岗位引用时会拒绝并说明原因 */
    void delete(Long id);
}
