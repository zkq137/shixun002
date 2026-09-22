package com.talent.employee.vo;

import lombok.Data;

/**
 * 批量操作结果。
 */
@Data
public class BatchResultVO {

    private int total;
    private int success;
    private int failed;

    public static BatchResultVO of(int total, int success) {
        BatchResultVO vo = new BatchResultVO();
        vo.setTotal(total);
        vo.setSuccess(success);
        vo.setFailed(total - success);
        return vo;
    }
}
