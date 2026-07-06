package com.example.interviewagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionTag {

    private Long id;
    private Long questionId;
    private Long tagId;
    private LocalDateTime createdAt;
}
