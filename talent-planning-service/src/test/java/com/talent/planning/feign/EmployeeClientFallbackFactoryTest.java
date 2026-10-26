package com.talent.planning.feign;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeClientFallbackFactoryTest {
    @Test
    void returnsUnified503WhenEmployeeServiceFails() {
        EmployeeClient client = new EmployeeClientFallbackFactory().create(new RuntimeException("连接失败"));

        var result = client.getEmployee(1L);

        assertEquals(503, result.getCode());
        assertTrue(result.getMessage().contains("连接失败"));
    }
}
