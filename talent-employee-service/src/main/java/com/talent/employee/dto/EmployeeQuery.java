package com.talent.employee.dto;

import lombok.Data;

/**
 * 员工列表查询条件。
 */
@Data
public class EmployeeQuery {

    /** 页码，从 1 开始 */
    private long pageNum = 1;

    /** 每页条数，最大 200 */
    private long pageSize = 10;

    /** 关键字：姓名或工号模糊匹配 */
    private String keyword;

    private String department;

    private Long positionId;

    /** 人才标签：核心骨干/储备人才/普通员工/待优化 */
    private String talentTag;

    /** 流失风险：低风险/中风险/高风险 */
    private String warningLevel;

    private String status;

    public long getPageNum() {
        return pageNum < 1 ? 1 : pageNum;
    }

    public long getPageSize() {
        if (pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 200);
    }
}
