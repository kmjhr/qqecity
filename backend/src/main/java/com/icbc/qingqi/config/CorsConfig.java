package com.icbc.qingqi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 * <p>
 * 同时支持用户前端、管理前端、微信小程序的跨域请求
 * 生产环境建议收紧 allowedOrigins
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的来源（开发环境放开，生产环境建议指定具体域名）
        config.addAllowedOriginPattern("*");
        // 允许携带凭证（Cookie/Authorization）
        config.setAllowCredentials(true);
        // 允许的请求方法
        config.addAllowedMethod("*");
        // 允许的请求头
        config.addAllowedHeader("*");
        // 暴露响应头（小程序需要读取自定义头时配置）
        config.addExposedHeader("Authorization");
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
