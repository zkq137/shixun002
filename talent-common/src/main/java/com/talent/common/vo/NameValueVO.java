package com.talent.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用的「名称-数值」结果，各模块的统计接口（部门人数、标签分布等）共用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameValueVO {

    private String name;
    private Long value;
}
