package com.talent.promotion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 模块四：晋升决策支持
 */
@SpringBootApplication
@EnableDiscoveryClient
public class PromotionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PromotionServiceApplication.class, args);
    }
}
