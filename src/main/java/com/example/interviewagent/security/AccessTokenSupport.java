package com.example.interviewagent.security;

import com.example.interviewagent.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Access Token 校验的公共逻辑，供各拦截器复用。
 * 从 Authorization: Bearer &lt;AT&gt; 取出令牌并校验签名/过期/类型，
 * 失败统一抛出 401 的 {@link BusinessException}（由全局异常处理器映射为真实 401）。
 */
@Component
@RequiredArgsConstructor
public class AccessTokenSupport {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 校验请求携带的 Access Token 并返回其声明。
     *
     * @throws BusinessException 401，当缺失、格式非法、签名不合法或已过期时
     */
    public Claims authenticate(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(401, "缺少或非法的 Access Token");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        try {
            return jwtTokenProvider.parseAndValidate(token, JwtTokenProvider.TYPE_ACCESS);
        } catch (JwtException e) {
            throw new BusinessException(401, "Access Token 无效或已过期");
        }
    }
}
