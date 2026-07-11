package com.example.interviewagent.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 通用认证拦截器：仅要求「持有有效 Access Token」，不校验角色。
 * 用于保护普通业务接口（如 /api/questions、/api/users 等）。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final AccessTokenSupport accessTokenSupport;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 校验 AT，失败抛 401（交由全局异常处理器返回真实 401 状态码）
        Claims claims = accessTokenSupport.authenticate(request);
        // 供下游控制器按需取用（如审计、按用户过滤）
        request.setAttribute("authUserId", Long.valueOf(claims.getSubject()));
        request.setAttribute("authRole", jwtTokenProvider.getRole(claims));
        return true;
    }
}
