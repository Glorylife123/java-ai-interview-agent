package com.example.interviewagent.security;

import com.example.interviewagent.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 题库、标签等后台资源的写操作授权拦截器。
 * <p>
 * GET 请求对所有已登录用户开放；POST / PUT / DELETE 仅 ADMIN 可执行。
 * 不能只依赖前端菜单和路由守卫：攻击者可以绕过浏览器界面直接调用 API，
 * 因此角色校验必须在服务端再次执行。
 */
@Component
@RequiredArgsConstructor
public class AdminWriteAuthInterceptor implements HandlerInterceptor {

    private static final String ROLE_ADMIN = "ADMIN";

    private final AccessTokenSupport accessTokenSupport;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        // 浏览题库/标签为读取操作，普通登录用户可访问；OPTIONS 用于 CORS 预检。
        if (HttpMethod.GET.matches(method) || HttpMethod.OPTIONS.matches(method)) {
            return true;
        }

        // 正常情况下 JwtAuthInterceptor 已经将 authRole 写进 request；
        // 保留 authenticate 兜底，确保即便注册顺序调整也不会失去认证校验。
        String role = (String) request.getAttribute("authRole");
        if (role == null) {
            role = jwtTokenProvider.getRole(accessTokenSupport.authenticate(request));
        }
        if (!ROLE_ADMIN.equals(role)) {
            throw new BusinessException(403, "需要管理员权限");
        }
        return true;
    }
}
