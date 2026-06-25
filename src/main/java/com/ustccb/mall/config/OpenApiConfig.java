package com.ustccb.mall.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 配置
 */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI mallOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Spring Boot Mall API")
                .description("Spring Boot 商城后端 - 展示 Java 后端工程能力（Cache Aside / Idempotent / 分布式锁 / AOP）")
                .version("1.0.0"));
    }
}
