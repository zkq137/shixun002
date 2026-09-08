package com.talent.planning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 模块二：梯队规划与预测
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class PlanningServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlanningServiceApplication.class, args);
    }
}
