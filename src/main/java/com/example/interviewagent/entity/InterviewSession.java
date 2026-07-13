package com.example.interviewagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试会话实体，对应数据库表 interview_session。
 * 表名绑定在 MyBatis XML 中；本项目未引入 MyBatis-Plus，故无 @TableName 注解。
 */
@Data
public class InterviewSession {

    private Long id;
    private Long userId;
    private String title;
    private String position;
    /** 难度：简单/中等/困难。 */
    private String difficulty;
    /** 状态：CREATED/IN_PROGRESS/FINISHED/CANCELLED。 */
    private String status;
    private Integer totalQuestionCount;
    /** 当前题目索引（从 0 开始），出题后自增。 */
    private Integer currentQuestionIndex;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
