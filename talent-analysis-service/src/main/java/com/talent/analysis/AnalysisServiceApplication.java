package com.talent.analysis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 模块五：数据分析与报告
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.talent.analysis.mapper")
public class AnalysisServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalysisServiceApplication.class, args);
    }
}
