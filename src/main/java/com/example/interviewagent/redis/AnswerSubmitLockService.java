package com.example.interviewagent.redis;

import com.example.interviewagent.config.RedisFeatureProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerSubmitLockService {

    private static final String ANSWER_SUBMIT_LOCK_PREFIX = "answer:submit:lock:";

    private final StringRedisTemplate redisTemplate;
    private final RedisFeatureProperties properties;

    public boolean tryLock(Long userId, Long questionId) {
        if (userId == null || questionId == null) {
            return true;
        }
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    key(userId, questionId), "1", properties.getAnswerSubmitLockTtl());
            return Boolean.TRUE.equals(acquired);
        } catch (RuntimeException e) {
            log.warn("写入答题防重复提交锁失败，本次降级放行：{}", e.getMessage());
            return true;
        }
    }

    private String key(Long userId, Long questionId) {
        return ANSWER_SUBMIT_LOCK_PREFIX + userId + ':' + questionId;
    }
}