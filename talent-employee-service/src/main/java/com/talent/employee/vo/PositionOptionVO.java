package com.talent.employee.vo;

import lombok.Data;

/**
 * 岗位下拉选项。
 */
@Data
public class PositionOptionVO {

    private Long id;
    private String positionName;
    private String department;
    private String levelTier;
}
