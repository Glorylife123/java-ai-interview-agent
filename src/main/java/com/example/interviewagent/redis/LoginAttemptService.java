package com.example.interviewagent.redis;

import com.example.interviewagent.config.RedisFeatureProperties;
import com.example.interviewagent.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String KEY_PREFIX = "interview:login:fail:";
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisFeatureProperties properties;

    public void ensureAllowed(String username, String clientIp) {
        try {
            String value = redisTemplate.opsForValue().get(key(username, clientIp));
            long failures = value == null ? 0L : Long.parseLong(value);
            if (failures >= properties.getLoginMaxFailures()) {
                throw new BusinessException(429, "登录失败次数过多，请稍后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("读取登录限流计数失败，本次登录降级放行：{}", e.getMessage());
        }
    }

    public long recordFailure(String username, String clientIp) {
        try {
            Long failures = redisTemplate.execute(
                    INCREMENT_SCRIPT,
                    List.of(key(username, clientIp)),
                    String.valueOf(properties.getLoginFailureWindow().toMillis())
            );
            return failures == null ? 0L : failures;
        } catch (RuntimeException e) {
            log.warn("记录登录失败次数失败，本次跳过限流计数：{}", e.getMessage());
            return 0L;
        }
    }

    public void clearFailures(String username, String clientIp) {
        try {
            redisTemplate.delete(key(username, clientIp));
        } catch (RuntimeException e) {
            log.warn("清理登录失败计数失败：{}", e.getMessage());
        }
    }

    private String key(String username, String clientIp) {
        String normalizedUsername = username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
        String normalizedIp = clientIp == null ? "unknown" : clientIp;
        return KEY_PREFIX + sha256(normalizedUsername + '\0' + normalizedIp);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }
}
