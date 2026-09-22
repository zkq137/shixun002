package com.talent.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.talent.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 公共模块的自动配置。
 *
 * <p>为什么要这个类：各个服务的启动类在 {@code com.talent.employee}、{@code com.talent.planning}
 * 这样的包下，Spring 只扫描启动类所在的包，扫不到 {@code com.talent.common}，
 * 所以 {@link GlobalExceptionHandler} 之前根本不会生效，抛异常走的是 Spring 默认错误页。
 * 通过 spring.factories 式的自动配置注册进来，各服务就不用改启动类了。
 *
 * <p>顺便把 MyBatis-Plus 的分页插件也在这里装上——不装的话 {@code page()} 会把整张表查出来，
 * 分页参数形同虚设。用 {@code @ConditionalOnClass} 保证只有引了 mybatis-plus 的模块生效。
 */
@AutoConfiguration
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnClass(MybatisPlusInterceptor.class)
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
