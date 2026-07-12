package com.example.interviewagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置（springdoc-openapi + Knife4j，对应 Spring Boot 3）。
 * <p>访问地址（两套等价 UI）：
 * <ul>
 *   <li>{@code /doc.html} —— Knife4j 国产文档 UI（推荐，界面更贴近中文习惯）；</li>
 *   <li>{@code /swagger-ui/index.html} —— springdoc 自带 Swagger UI。</li>
 * </ul>
 * 两套 UI 共用同一份 {@code /v3/api-docs} 接口定义，无需重复配置。</p>
 *
 * 文档接口全程放行（见 {@link WebConfig} 的 AUTH_WHITELIST），不要求 AT；
 * 在线调试需要在前端 UI 里手动给需要鉴权的接口填入 Bearer Token。
 */
@Configuration
public class OpenApiConfig {

    /** JWT Bearer 认证方案名称，供 Swagger UI 在线调试时填写。 */
    public static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java AI 面试助手 API")
                        .description("面试题库、答题记录、错题本、练习统计等接口文档。需鉴权接口请在右上角 Authorize 填入 Bearer Access Token。")
                        .version("v1"))
                // 全局安全方案：所有接口默认支持 Bearer Token 认证
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")));
    }
}
