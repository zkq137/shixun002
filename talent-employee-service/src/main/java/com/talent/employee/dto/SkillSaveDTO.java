package com.talent.employee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增/修改技能节点。
 */
@Data
public class SkillSaveDTO {

    @NotBlank(message = "技能名称不能为空")
    private String skillName;

    /** 0 或空 = 新建一个顶级分类 */
    private Long parentId;

    private String description;

    private String status;

    private Integer sortOrder;
}
