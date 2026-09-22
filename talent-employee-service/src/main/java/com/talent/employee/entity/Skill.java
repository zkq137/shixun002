package com.talent.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 技能（技能树的一个节点），对应表 skill。
 *
 * <p>两层结构：parent_id = 0 的是顶级分类（level=1），挂在分类下面的是具体技能（level=2）。
 * 员工会的技能（emp_skill）和岗位要求的技能（pos_skill_require）都引用这里的 id。
 */
@Data
@TableName("skill")
public class Skill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String skillName;

    /** 父节点ID，0 表示顶级分类 */
    private Long parentId;

    /** 层级：1=分类，2=技能 */
    private Integer level;

    /** 同级排序 */
    private Integer sortOrder;

    /** 启用 / 停用 */
    private String status;

    private String description;

    /** 分类名称（冗余字段，跟着父节点走） */
    private String skillCategory;

    private LocalDateTime createdAt;
}
