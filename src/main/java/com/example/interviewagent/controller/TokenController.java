package com.example.interviewagent.controller;

import com.example.interviewagent.common.Result;
import com.example.interviewagent.config.JwtProperties;
import com.example.interviewagent.controller.dto.RefreshResponse;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.security.RefreshTokenCookieFactory;
import com.example.interviewagent.service.TokenService;
import com.example.interviewagent.service.dto.IssuedTokens;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 令牌相关接口：刷新与注销。RT 一律从 Cookie 读取，不接受请求体传入。
 * 统一挂在 /api/auth 前缀下，与登录接口同属 auth 模块。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final RefreshTokenCookieFactory cookieFactory;
    private final JwtProperties jwtProperties;

    /**
     * 刷新接口：从 Cookie 读取旧 RT，轮换后颁发新的 AT + RT。
     * 新 RT 通过 Cookie 覆盖下发，响应体仅返回新的 AT 与过期时间。
     */
    @PostMapping("/refresh")
    public Result<RefreshResponse> refresh(
            @CookieValue(name = "${app.jwt.cookie-name}", required = false) String refreshToken,
            HttpServletResponse response) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(401, "缺少 Refresh Token");
        }

        // 校验签名/过期/吊销状态并轮换（含宽限期逻辑，均在 Service 层事务内完成）
        IssuedTokens tokens = tokenService.refresh(refreshToken);

        // 用新 RT 覆盖旧 Cookie
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.build(tokens.refreshToken()).toString());

        return Result.success(new RefreshResponse(tokens.accessToken(), tokens.accessExpiresIn()));
    }

    /**
     * 注销接口：将当前设备持有的 RT 标记为失效，并清除 Cookie。
     */
    @PostMapping("/logout")
    public Result<Void> logout( // 完整路径：/api/auth/logout
            @CookieValue(name = "${app.jwt.cookie-name}", required = false) String refreshToken,
            HttpServletResponse response) {
        tokenService.logout(refreshToken);
        // 无论是否存在 RT，都下发清除 Cookie，保证客户端本地状态一致
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString());
        return Result.success();
    }
}
