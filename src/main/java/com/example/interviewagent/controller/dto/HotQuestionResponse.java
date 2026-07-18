package com.example.interviewagent.controller.dto;

/** Redis ZSet 中的热门题目及其当前热度。 */
public record HotQuestionResponse(Long questionId, String title, long viewScore) {
}
