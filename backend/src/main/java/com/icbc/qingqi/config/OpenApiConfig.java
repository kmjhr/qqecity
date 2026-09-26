package com.icbc.qingqi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 配置
 * <p>
 * - 配置全局 Bearer Token 鉴权方案，Swagger UI 可点 Authorize 输入 Token
 * - 全局生效：所有接口默认需要 Authorization: Bearer <token>
 * - 登录/注册接口在 JwtAuthFilter 白名单中放行，无需 Token
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("青启e城 后端接口文档")
                        .description("青启e城项目后端 API，覆盖用户、保函、贷款、预算、记账、安全、消息 7 大模块。" +
                                "鉴权方式：JWT，请求头 Authorization: Bearer <token>。" +
                                "免登录接口：/api/v1/auth/**。")
                        .version("v1.0.0")
                        .contact(new Contact().name("青启e城").email("qingqi@example.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("请输入登录接口返回的 accessToken，无需添加 'Bearer ' 前缀")));
    }
}
