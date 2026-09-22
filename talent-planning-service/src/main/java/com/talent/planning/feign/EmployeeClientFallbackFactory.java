package com.talent.planning.feign;

import com.talent.common.result.Result;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 员工服务调不通时的降级实现。
 *
 * <p>触发时机：员工服务没有健康实例、调用超时，或在 Sentinel 控制台给
 * {@code talent-employee-service} 这个 Feign 资源配了熔断规则并已打开。
 * 没有配规则时 Sentinel 不会主动熔断，只会记录调用情况，属正常现象。
 *
 * <p>注意：Spring 只会把回调里返回的对象当成降级实现，所以这里不用写 null 判断。
 */
@Component
public class EmployeeClientFallbackFactory implements FallbackFactory<EmployeeClient> {

    @Override
    public EmployeeClient create(Throwable cause) {
        return id -> Result.fail(503, "员工服务暂时不可用，已降级返回；原因：" + cause.getMessage());
    }
}
