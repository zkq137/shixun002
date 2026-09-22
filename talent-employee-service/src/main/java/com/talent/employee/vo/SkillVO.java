package com.talent.employee.vo;

import lombok.Data;

/**
 * 技能节点（平铺），带被引用的次数。
 */
@Data
public class SkillVO {

    private Long id;
    private String skillName;
    private Long parentId;
    private Integer level;
    private Integer sortOrder;
    private String status;
    private String description;
    private String skillCategory;

    /** 有多少员工会这个技能 */
    private Integer employeeCount;

    /** 有多少个岗位要求这个技能 */
    private Integer positionCount;

    /** 分类下有几个子技能（只有分类节点用得到） */
    private Integer childCount;
}
