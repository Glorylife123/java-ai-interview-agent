package com.example.interviewagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试会话摘要（用于历史列表）。
 */
@Data
public class InterviewSessionVO {

    private Long id;
    private String title;
    private String position;
    private String difficulty;
    private String status;
    private Integer totalQuestionCount;
    private Integer currentQuestionIndex;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
