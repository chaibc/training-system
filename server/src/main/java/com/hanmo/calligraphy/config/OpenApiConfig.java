package com.hanmo.calligraphy.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 文档配置。
 *
 * <p>声明接口文档标题、版本与 Bearer JWT 鉴权方案，使 Swagger UI 可以填入令牌调试需要登录的接口。</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * 构建 OpenAPI 文档定义。
     *
     * @return 带全局 BearerAuth 安全要求的 OpenAPI 对象
     */
    @Bean
    public OpenAPI hanmoOpenApi() {
        String schemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("翰墨学堂书法培训管理系统 API")
                        .description("管理后台与微信小程序接口")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
