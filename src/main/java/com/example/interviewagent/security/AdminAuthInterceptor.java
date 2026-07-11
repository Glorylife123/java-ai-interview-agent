package com.example.interviewagent.security;

import com.example.interviewagent.exception.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端接口鉴权拦截器（不依赖 Spring Security 过滤器链）。
 * <p>
 * 在通用 AT 校验的基础上，额外要求角色为 ADMIN 方可放行。
 * 抛出的 {@link BusinessException} 由全局异常处理器统一返回。
 */
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final String ROLE_ADMIN = "ADMIN";

    private final AccessTokenSupport accessTokenSupport;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 校验 Access Token（缺失/非法/过期 → 401）
        Claims claims = accessTokenSupport.authenticate(request);

        // 2. 角色校验：非 ADMIN → 403
        String role = jwtTokenProvider.getRole(claims);
        if (!ROLE_ADMIN.equals(role)) {
            throw new BusinessException(403, "需要管理员权限");
        }

        // 便于后续 Controller 取用（如审计日志）
        request.setAttribute("authUserId", Long.valueOf(claims.getSubject()));
        request.setAttribute("authRole", role);
        return true;
    }
}
