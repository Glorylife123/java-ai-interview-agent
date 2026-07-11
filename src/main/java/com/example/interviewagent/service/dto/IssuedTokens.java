package com.example.interviewagent.service.dto;

/**
 * 一次签发的双令牌结果。
 *
 * @param accessToken           Access Token
 * @param accessExpiresIn       Access Token 有效期（秒），用于响应体 expires_in
 * @param refreshToken          Refresh Token（通过 HttpOnly Cookie 下发）
 * @param refreshMaxAgeSeconds  Refresh Token 有效期（秒），用于 Cookie 的 Max-Age
 */
public record IssuedTokens(
        String accessToken,
        long accessExpiresIn,
        String refreshToken,
        long refreshMaxAgeSeconds
) {
}
