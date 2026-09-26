package com.icbc.qingqi.config;

import com.icbc.qingqi.security.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 安全配置 - 注册 JWT 过滤器
 * <p>
 * 说明：本骨架未使用 Spring Security（太重，学习成本高），
 * 而是用自定义 JWT 过滤器实现鉴权，代码量少、AI 易理解。
 * 如果后续需要更复杂的安全能力（OAuth2、方法级注解鉴权等），
 * 可平滑切换到 Spring Security。
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthFilter);
        registration.addUrlPatterns("/*");
        registration.setName("jwtAuthFilter");
        // 优先级：在其他业务过滤器之前执行
        registration.setOrder(1);
        return registration;
    }
}
