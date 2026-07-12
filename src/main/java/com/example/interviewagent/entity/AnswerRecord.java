package com.example.interviewagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 答题记录实体，对应数据库表 answer_record。
 * 表名绑定在 MyBatis XML（AnswerRecordMapper.xml）的 SQL 中；本项目未引入 MyBatis-Plus，故无 @TableName 注解。
 */
@Data
public class AnswerRecord {

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
