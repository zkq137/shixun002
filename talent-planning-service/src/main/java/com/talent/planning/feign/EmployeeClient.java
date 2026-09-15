package com.talent.planning.feign;

import com.talent.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 通过 Nacos 服务发现，调用员工服务。演示跨服务调用。
 * fallbackFactory 指定降级实现：员工服务挂掉或触发熔断时，不再抛异常，而是返回兜底数据。
 * 前提是 application.yml 里打开了 feign.sentinel.enabled。
 */
@FeignClient(name = "talent-employee-service", fallbackFactory = EmployeeClientFallbackFactory.class)
public interface EmployeeClient {

    @GetMapping("/api/employee/{id}")
    Result<Map<String, Object>> getEmployee(@PathVariable("id") Long id);
}
