package com.example.interviewagent.redis;

import com.example.interviewagent.controller.dto.QuestionResponse;

/** hit=true 且 value=null 表示命中了短 TTL 的空值缓存。 */
public record QuestionCacheLookup(boolean hit, QuestionResponse value) {

    public static QuestionCacheLookup miss() {
        return new QuestionCacheLookup(false, null);
    }

    public static QuestionCacheLookup negativeHit() {
        return new QuestionCacheLookup(true, null);
    }

    public static QuestionCacheLookup valueHit(QuestionResponse value) {
        return new QuestionCacheLookup(true, value);
    }
}
