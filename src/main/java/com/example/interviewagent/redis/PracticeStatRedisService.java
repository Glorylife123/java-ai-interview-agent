package com.example.interviewagent.redis;

import com.example.interviewagent.config.RedisFeatureProperties;
import com.example.interviewagent.vo.PracticeStatVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PracticeStatRedisService {

    private static final String PRACTICE_STAT_PREFIX = "user:practice:stat:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisFeatureProperties properties;

    public Optional<PracticeStatVO> findOverview(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        String key = key(userId);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, PracticeStatVO.class));
        } catch (JsonProcessingException e) {
            log.warn("练习统计缓存反序列化失败，删除损坏缓存。key={}", key);
            evictOverview(userId);
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("读取练习统计缓存失败，已回源 MySQL：{}", e.getMessage());
            return Optional.empty();
        }
    }

    public void putOverview(Long userId, PracticeStatVO stat) {
        if (userId == null || stat == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(userId), objectMapper.writeValueAsString(stat), properties.getPracticeStatTtl());
        } catch (JsonProcessingException e) {
            log.warn("练习统计序列化失败，本次跳过缓存。userId={}", userId);
        } catch (RuntimeException e) {
            log.warn("写入练习统计缓存失败，已跳过：{}", e.getMessage());
        }
    }

    public void evictOverview(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redisTemplate.delete(key(userId));
        } catch (RuntimeException e) {
            log.warn("删除练习统计缓存失败，已跳过：{}", e.getMessage());
        }
    }

    private String key(Long userId) {
        return PRACTICE_STAT_PREFIX + userId;
    }
}