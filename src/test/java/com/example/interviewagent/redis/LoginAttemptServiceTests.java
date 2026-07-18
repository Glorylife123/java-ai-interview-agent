package com.example.interviewagent.redis;

import com.example.interviewagent.config.RedisFeatureProperties;
import com.example.interviewagent.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTests {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        RedisFeatureProperties properties = new RedisFeatureProperties();
        properties.setLoginFailureWindow(Duration.ofMinutes(5));
        properties.setLoginMaxFailures(5);
        loginAttemptService = new LoginAttemptService(redisTemplate, properties);
    }

    @Test
    void blocksLoginAfterConfiguredFailureCount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("5");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> loginAttemptService.ensureAllowed("alice", "127.0.0.1"));

        assertEquals(429, exception.getCode());
    }

    @Test
    void redisFailureDoesNotBreakCredentialLogin() {
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));

        loginAttemptService.ensureAllowed("alice", "127.0.0.1");
    }
}
