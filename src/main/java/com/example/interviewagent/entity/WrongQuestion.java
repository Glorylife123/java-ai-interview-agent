package com.example.interviewagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WrongQuestion {

    private Long id;
    private Long userId;
    private Long questionId;
    private Integer wrongCount;
    private LocalDateTime lastWrongAt;
    private Integer mastered;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
