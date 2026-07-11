package com.example.interviewagent.security;

import com.example.interviewagent.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 负责构建下发 Refresh Token 的 Cookie。
 * 统一设置 HttpOnly / Secure / SameSite=Strict，避免各处重复且易漏配安全属性。
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookieFactory {

    private final JwtProperties properties;

    /** 构建携带 RT 的 Cookie。 */
    public ResponseCookie build(String refreshToken) {
        return ResponseCookie.from(properties.getCookieName(), refreshToken)
                .httpOnly(true)                              // 禁止 JS 读取，防 XSS 窃取
                .secure(properties.isCookieSecure())         // 仅 HTTPS 下发（dev 环境关闭）
                .sameSite(properties.getCookieSameSite())    // Strict，防 CSRF
                .path("/")                                   // 覆盖 /auth/refresh 与 /logout
                .maxAge(properties.getRefreshTokenTtlSeconds())
                .build();
    }

    /** 构建用于清除 RT 的 Cookie（注销时下发，Max-Age=0 立即失效）。 */
    public ResponseCookie clear() {
        return ResponseCookie.from(properties.getCookieName(), "")
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path("/")
                .maxAge(0)
                .build();
    }
}
