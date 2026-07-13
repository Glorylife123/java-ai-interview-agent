package com.example.interviewagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试答案实体，对应数据库表 interview_answer。
 * 一个题目记录（question_record_id）至多对应一条答案，用于防止重复提交。
 */
@Data
public class InterviewAnswer {

    private Long id;
    private Long sessionId;
    private Long questionRecordId;
    private Long userId;
    private String answerContent;
    private Integer durationSeconds;
    private LocalDateTime createdAt;
}
