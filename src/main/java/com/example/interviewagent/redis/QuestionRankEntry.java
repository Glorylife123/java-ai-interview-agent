package com.example.interviewagent.redis;

/** Redis ZSet 排行项。 */
public record QuestionRankEntry(Long questionId, long score) {
}
