package com.example.interviewagent.controller.dto;

import com.example.interviewagent.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应体。
 * Refresh Token 只通过 HttpOnly Cookie 下发，绝不出现在响应体里（防 XSS 窃取）。
 * user 为登录用户信息（不含密码哈希），供前端展示与本地态存储使用。
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    @JsonProperty("access_token")
    private String accessToken;

    /** Access Token 有效期（秒）。 */
    @JsonProperty("expires_in")
    private long expiresIn;

    /** 登录用户信息（passwordHash 已置空）。 */
    private User user;
}
