package com.example.interviewagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAnswerRecord {

    private Long id;
    private Long userId;
    private Long questionId;
    private String userAnswer;
    private Integer isCorrect;
    private Integer score;
    private Integer timeCostSeconds;
    private String answerSource;
    private LocalDateTime createdAt;
}
