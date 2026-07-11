package com.example.interviewagent.security;

import java.time.LocalDateTime;

/**
 * 签发后的 Refresh Token 载体：包含令牌字符串与其过期时间，
 * 便于业务层在持久化 RT 时写入 expires_at。
 */
public record SignedRefreshToken(String token, LocalDateTime expiresAt) {
}
