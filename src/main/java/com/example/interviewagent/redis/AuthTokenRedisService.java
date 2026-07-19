package com.example.interviewagent.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenRedisService {

    private static final String LOGIN_TOKEN_PREFIX = "login:token:";

    private final StringRedisTemplate redisTemplate;

    public void cacheAccessToken(Long userId, String accessToken, Duration ttl) {
        if (userId == null || accessToken == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(userId), accessToken, ttl);
        } catch (RuntimeException e) {
            log.warn("写入登录 token 缓存失败，已跳过：{}", e.getMessage());
        }
    }

    public void evictAccessToken(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redisTemplate.delete(key(userId));
        } catch (RuntimeException e) {
            log.warn("删除登录 token 缓存失败，已跳过：{}", e.getMessage());
        }
    }

    private String key(Long userId) {
        return LOGIN_TOKEN_PREFIX + userId;
    }
}