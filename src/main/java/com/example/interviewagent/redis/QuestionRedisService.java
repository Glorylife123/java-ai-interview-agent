package com.example.interviewagent.redis;

import com.example.interviewagent.config.RedisFeatureProperties;
import com.example.interviewagent.controller.dto.QuestionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionRedisService {

    private static final String DETAIL_PREFIX = "interview:question:detail:";
    private static final String VIEW_RANK_KEY = "interview:rank:question:view";
    private static final String NULL_MARKER = "__NULL__";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisFeatureProperties properties;

    public QuestionCacheLookup findDetail(Long questionId) {
        String key = detailKey(questionId);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return QuestionCacheLookup.miss();
            }
            if (NULL_MARKER.equals(value)) {
                return QuestionCacheLookup.negativeHit();
            }
            return QuestionCacheLookup.valueHit(objectMapper.readValue(value, QuestionResponse.class));
        } catch (JsonProcessingException e) {
            log.warn("题目缓存反序列化失败，删除损坏缓存。key={}", key);
            evictDetail(questionId);
            return QuestionCacheLookup.miss();
        } catch (RuntimeException e) {
            logRedisFallback("读取题目缓存", e);
            return QuestionCacheLookup.miss();
        }
    }

    public void putDetail(Long questionId, QuestionResponse response) {
        try {
            String value = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(detailKey(questionId), value, detailTtlWithJitter());
        } catch (JsonProcessingException e) {
            log.warn("题目详情序列化失败，本次跳过缓存。questionId={}", questionId);
        } catch (RuntimeException e) {
            logRedisFallback("写入题目缓存", e);
        }
    }

    public void putNotFound(Long questionId) {
        try {
            redisTemplate.opsForValue().set(
                    detailKey(questionId), NULL_MARKER, properties.getQuestionNullTtl());
        } catch (RuntimeException e) {
            logRedisFallback("写入题目空值缓存", e);
        }
    }

    public void evictDetail(Long questionId) {
        try {
            redisTemplate.delete(detailKey(questionId));
        } catch (RuntimeException e) {
            logRedisFallback("删除题目缓存", e);
        }
    }

    /** 标签名称或分类变化会影响所有缓存详情中的嵌套标签，因此使用 SCAN 分批清理。 */
    public void evictAllDetails() {
        List<String> batch = new ArrayList<>(100);
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
                .match(DETAIL_PREFIX + "*")
                .count(100)
                .build())) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() == 100) {
                    redisTemplate.delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                redisTemplate.delete(batch);
            }
        } catch (RuntimeException e) {
            logRedisFallback("批量删除题目缓存", e);
        }
    }

    public void incrementViewRank(Long questionId) {
        try {
            redisTemplate.opsForZSet().incrementScore(VIEW_RANK_KEY, questionId.toString(), 1D);
        } catch (RuntimeException e) {
            logRedisFallback("累加题目热度", e);
        }
    }

    public void removeFromViewRank(Long questionId) {
        try {
            redisTemplate.opsForZSet().remove(VIEW_RANK_KEY, questionId.toString());
        } catch (RuntimeException e) {
            logRedisFallback("删除题目排行项", e);
        }
    }

    public List<QuestionRankEntry> topViewed(int limit) {
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(VIEW_RANK_KEY, 0, limit - 1L);
            if (tuples == null || tuples.isEmpty()) {
                return List.of();
            }
            List<QuestionRankEntry> result = new ArrayList<>(tuples.size());
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                try {
                    Long questionId = Long.valueOf(tuple.getValue());
                    Double score = tuple.getScore();
                    result.add(new QuestionRankEntry(questionId, score == null ? 0L : score.longValue()));
                } catch (NumberFormatException ignored) {
                    log.warn("忽略无法识别的题目排行 member：{}", tuple.getValue());
                }
            }
            return result;
        } catch (RuntimeException e) {
            logRedisFallback("读取题目排行榜", e);
            return List.of();
        }
    }

    private Duration detailTtlWithJitter() {
        long jitterMillis = Math.max(0L, properties.getQuestionTtlJitter().toMillis());
        if (jitterMillis == 0L) {
            return properties.getQuestionDetailTtl();
        }
        return properties.getQuestionDetailTtl()
                .plusMillis(ThreadLocalRandom.current().nextLong(jitterMillis + 1L));
    }

    private String detailKey(Long questionId) {
        return DETAIL_PREFIX + questionId;
    }

    private void logRedisFallback(String operation, RuntimeException e) {
        log.warn("{}失败，已降级为不依赖 Redis 的路径：{}", operation, e.getMessage());
    }
}
