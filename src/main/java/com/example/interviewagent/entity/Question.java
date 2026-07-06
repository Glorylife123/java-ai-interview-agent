package com.example.interviewagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Question {

    private Long id;
    private String title;
    private String content;
    private String questionType;
    private Integer difficulty;
    private String answer;
    private String answerAnalysis;
    private String source;
    private Integer viewCount;
    private Integer submitCount;
    private Integer correctCount;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
