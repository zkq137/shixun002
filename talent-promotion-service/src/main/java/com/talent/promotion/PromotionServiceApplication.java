package com.talent.promotion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 模块四：晋升决策支持
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.talent.promotion.mapper")
public class PromotionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PromotionServiceApplication.class, args);
    }
}
