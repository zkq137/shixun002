package com.talent.planning.feign;

import com.talent.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 通过 Nacos 服务发现，调用员工服务。演示跨服务调用。
 */
@FeignClient(name = "talent-employee-service")
public interface EmployeeClient {

    @GetMapping("/api/employee/{id}")
    Result<Map<String, Object>> getEmployee(@PathVariable("id") Long id);
}
