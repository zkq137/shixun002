package com.talent.planning.feign;

import com.talent.common.result.Result;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class EmployeeClientFallbackFactory implements FallbackFactory<EmployeeClient> {
    @Override
    public EmployeeClient create(Throwable cause) {
        String reason = cause == null || cause.getMessage() == null ? "未知原因" : cause.getMessage();
        return id -> Result.fail(503, "员工服务暂不可用：" + reason);
    }
}
