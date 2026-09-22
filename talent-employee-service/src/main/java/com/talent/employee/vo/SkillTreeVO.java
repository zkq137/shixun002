package com.talent.employee.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能树节点（分类 + 子技能），给前端的树控件用。
 */
@Data
public class SkillTreeVO {

    private Long id;
    private String skillName;
    private Long parentId;
    private Integer level;
    private Integer sortOrder;
    private String status;
    private String description;
    private String skillCategory;

    private Integer employeeCount;
    private Integer positionCount;

    private List<SkillTreeVO> children = new ArrayList<>();
}
