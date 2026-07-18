package com.example.interviewagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.redis")
public class RedisFeatureProperties {

    private Duration questionDetailTtl = Duration.ofMinutes(15);
    private Duration questionNullTtl = Duration.ofMinutes(1);
    private Duration questionTtlJitter = Duration.ofMinutes(5);
    private Duration loginFailureWindow = Duration.ofMinutes(5);
    private int loginMaxFailures = 5;
}
