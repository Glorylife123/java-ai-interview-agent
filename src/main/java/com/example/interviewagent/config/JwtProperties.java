package com.example.interviewagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 双令牌相关配置项，绑定 application.yml 中 app.jwt.* 的配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HS256 签名密钥，长度必须 >= 32 字节（256 位）。 */
    private String secret;

    /** Access Token 有效期（分钟），默认 15 分钟。 */
    private int accessTokenMinutes = 15;

    /** Refresh Token 有效期（天），默认 7 天。 */
    private int refreshTokenDays = 7;

    /** 刷新宽限期（秒），默认 30 秒：RT 失效后该时间窗内的重放视为网络重传。 */
    private long graceSeconds = 30;

    /** 下发 RT 的 Cookie 名称。 */
    private String cookieName = "refresh_token";

    /** Cookie 是否带 Secure 属性（仅 HTTPS 下发）。 */
    private boolean cookieSecure = true;

    /** Cookie 的 SameSite 属性。 */
    private String cookieSameSite = "Strict";

    /** Access Token 有效期（秒），用于响应体中的 expires_in。 */
    public long getAccessTokenTtlSeconds() {
        return accessTokenMinutes * 60L;
    }

    /** Refresh Token 有效期（秒），用于 Cookie 的 Max-Age。 */
    public long getRefreshTokenTtlSeconds() {
        return refreshTokenDays * 24L * 60L * 60L;
    }
}
