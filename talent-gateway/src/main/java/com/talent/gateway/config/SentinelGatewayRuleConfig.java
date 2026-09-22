package com.talent.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * 网关限流规则示例（入门演示用）。
 *
 * <p>这里用代码加载两条规则，方便直接看到效果；正式开发时更推荐在 Sentinel 控制台里配，
 * 或者把规则放到 Nacos 由 spring.cloud.sentinel.datasource 动态下发（见 README 第八节）。
 *
 * <p>规则一：自定义 API 分组 employee_api，匹配 /api/employee/**，每秒最多 20 次。
 * <p>规则二：按网关路由 ID 直接限流，talent-training 路由每秒最多 20 次。
 *
 * <p>被限流的请求会走 application.yml 里 spring.cloud.sentinel.scg.fallback 配置的 429 响应。
 *
 * <p>这两个阈值是为了演示能打出 429 才设得比较低，正式用之前按自己的容量改大。
 */
@Configuration
public class SentinelGatewayRuleConfig {

    /** 每秒允许通过的请求数，想演示限流效果就调小 */
    private static final double QPS = 20;

    @PostConstruct
    public void init() {
        initApiDefinitions();
        initGatewayRules();
    }

    /**
     * 自定义 API 分组：把一组路径当成一个资源来限流，比按路由 ID 更灵活。
     */
    private void initApiDefinitions() {
        Set<ApiPredicateItem> predicateItems = new HashSet<>();
        predicateItems.add(new ApiPathPredicateItem()
                .setPattern("/api/employee/**")
                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));

        ApiDefinition employeeApi = new ApiDefinition("employee_api")
                .setPredicateItems(predicateItems);

        GatewayApiDefinitionManager.loadApiDefinitions(Set.of(employeeApi));
    }

    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 按自定义 API 分组限流
        rules.add(new GatewayFlowRule("employee_api")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(QPS)
                .setIntervalSec(1));

        // 按路由 ID 限流（路由 ID 见 application.yml 里的 spring.cloud.gateway.routes）
        rules.add(new GatewayFlowRule("talent-training")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID)
                .setCount(QPS)
                .setIntervalSec(1));

        GatewayRuleManager.loadRules(rules);
    }
}
