package com.example.interviewagent.controller;

import com.example.interviewagent.common.Result;
import com.example.interviewagent.controller.dto.LoginResponse;
import com.example.interviewagent.entity.User;
import com.example.interviewagent.security.RefreshTokenCookieFactory;
import com.example.interviewagent.service.AuthService;
import com.example.interviewagent.service.TokenService;
import com.example.interviewagent.service.dto.IssuedTokens;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final RefreshTokenCookieFactory cookieFactory;

    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        User user = authService.register(
                request.getUsername(),
                request.getPassword(),
                request.getNickname()
        );
        user.setPasswordHash(null);
        return Result.success(user);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request,
                                       @RequestHeader(value = "X-Device-Id", required = false, defaultValue = "default")
                                       String deviceId,
                                       HttpServletResponse response) {
        // 1. 校验用户名密码（BCrypt），成功返回不含密码哈希的用户
        User user = authService.login(request.getUsername(), request.getPassword());

        // 2. 登录成功：同时签发 AT 与 RT，并将 RT 落库
        IssuedTokens tokens = tokenService.issueTokens(user, deviceId);

        // 3. RT 通过 HttpOnly + Secure + SameSite=Strict 的 Cookie 下发给客户端
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.build(tokens.refreshToken()).toString());

        // 4. 响应体只返回 AT / 过期秒数 / 用户信息；RT 仅经 HttpOnly Cookie 下发，绝不入响应体
        //    user.passwordHash 已在 authService.login 内置空，可安全返回给前端
        LoginResponse body = new LoginResponse(
                tokens.accessToken(),
                tokens.accessExpiresIn(),
                user
        );
        return Result.success(body);
    }

    /**
     * 修改密码。该接口不在鉴权白名单中，会被 JwtAuthInterceptor 拦截校验，
     * 因此可从请求属性 authUserId 拿到当前登录用户，用户只能改自己的密码。
     * 改密成功后，Service 层会吊销该用户全部 RT，其它设备/标签页下次刷新即被登出。
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request,
                                       @RequestAttribute("authUserId") Long userId) {
        authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String nickname;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
