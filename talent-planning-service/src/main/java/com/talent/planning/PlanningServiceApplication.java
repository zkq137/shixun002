package com.talent.planning;

import com.talent.common.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

/**
 * 模块二：梯队规划与预测
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@Import(GlobalExceptionHandler.class)
public class PlanningServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlanningServiceApplication.class, args);
    }
}
