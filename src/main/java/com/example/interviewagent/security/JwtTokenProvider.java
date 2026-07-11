package com.example.interviewagent.security;

import com.example.interviewagent.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类：基于 jjwt 0.12.x，使用 HS256 签名算法。
 * 负责签发 Access Token / Refresh Token，以及解析与验证签名。
 * <p>
 * 通过 token_type 声明区分两类令牌，避免 RT 被当作 AT 使用（反之亦然）。
 */
@Component
public class JwtTokenProvider {

    /** 令牌类型：访问令牌。 */
    public static final String TYPE_ACCESS = "access";
    /** 令牌类型：刷新令牌。 */
    public static final String TYPE_REFRESH = "refresh";

    private static final String CLAIM_TYPE = "token_type";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_DEVICE = "device_id";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        // HS256 要求密钥不少于 256 位（32 字节），否则 jjwt 会抛 WeakKeyException
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 Access Token（有效期取配置，默认 15 分钟）。
     * 载荷中携带 userId(subject) 与 role，供接口鉴权使用。
     */
    public String createAccessToken(Long userId, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getAccessTokenTtlSeconds());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 签发 Refresh Token（有效期取配置，默认 7 天）。
     * jti 由业务层生成并传入，以便构建轮换链（parent_jti）与数据库落库保持一致。
     */
    public SignedRefreshToken createRefreshToken(Long userId, String deviceId, String jti) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(properties.getRefreshTokenTtlSeconds());
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .id(jti)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_DEVICE, deviceId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new SignedRefreshToken(token, toLocalDateTime(exp));
    }

    /**
     * 解析并验证令牌：校验签名与过期时间，并校验 token_type 是否匹配期望类型。
     * 校验失败（签名不合法/已过期/类型不符）会抛出 {@link JwtException}。
     */
    public Claims parseAndValidate(String token, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertType(claims, expectedType);
        return claims;
    }

    /**
     * 解析令牌但允许其已过期（用于注销场景：即便 RT 过期也应能取出 jti 去吊销记录）。
     * 仍会校验签名与令牌类型。
     */
    public Claims parseAllowExpired(String token, String expectedType) {
        try {
            return parseAndValidate(token, expectedType);
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            assertType(claims, expectedType);
            return claims;
        }
    }

    /** 从声明中读取角色。 */
    public String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    private void assertType(Claims claims, String expectedType) {
        String actualType = claims.get(CLAIM_TYPE, String.class);
        if (!expectedType.equals(actualType)) {
            throw new JwtException("令牌类型不匹配，期望 " + expectedType + "，实际 " + actualType);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
