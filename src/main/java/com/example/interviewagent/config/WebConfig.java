package com.example.interviewagent.config;

import com.example.interviewagent.security.AdminAuthInterceptor;
import com.example.interviewagent.security.AdminWriteAuthInterceptor;
import com.example.interviewagent.security.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置：注册两级鉴权拦截器（不依赖 Spring Security 过滤器链）。
 * <ul>
 *   <li>通用认证拦截器：保护全部 /api/** 业务接口，放行认证类公共接口与管理端（后者由下方单独处理）；</li>
 *   <li>管理端拦截器：/api/admin/** 在认证基础上额外要求 ADMIN 角色。</li>
 * </ul>
 * 静态资源（/login.html、/app.js 等）与首页 / 不在 /api 前缀下，天然放行。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /** 认证类公共接口：登录/注册/刷新/注销，无需持有 AT。 */
    private static final String[] AUTH_WHITELIST = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/logout",
            // OpenAPI / Swagger UI 文档与接口定义 JSON：完全公开，便于在线查看与调试
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            // Knife4j 国产文档 UI：入口 /doc.html 及其静态资源（webjars）
            "/doc.html",
            "/webjars/**",
            "/favicon.ico",
    };

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final AdminWriteAuthInterceptor adminWriteAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 通用认证：拦截所有 /api/**，放行公共认证接口；/api/admin/** 交由下方管理端拦截器（避免重复校验）
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(AUTH_WHITELIST)
                .excludePathPatterns("/api/admin/**");

        // 管理端：在认证基础上额外要求 ADMIN 角色
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**");

        // 题库和标签：读操作允许所有已登录用户；写操作（POST / PUT / DELETE）仅 ADMIN 可执行。
        // 前端路由守卫只能改善体验，真正的权限边界必须在服务端执行。
        registry.addInterceptor(adminWriteAuthInterceptor)
                .addPathPatterns("/api/questions/**", "/api/tags/**");
    }
}
