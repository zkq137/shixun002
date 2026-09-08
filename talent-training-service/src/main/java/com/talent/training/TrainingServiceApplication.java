package com.talent.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 模块三：智能培训推荐
 */
@SpringBootApplication
@EnableDiscoveryClient
public class TrainingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrainingServiceApplication.class, args);
    }
}
