package com.example.interviewagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试题目记录实体，对应数据库表 interview_question_record。
 * question_content 为出题时从 question.content 复制的快照，防止题库后续修改影响面试记录。
 */
@Data
public class InterviewQuestionRecord {

    private Long id;
    private Long sessionId;
    private Long questionId;
    /** 题目内容快照。 */
    private String questionContent;
    /** 考察能力（本规则版用 question 的题型/分类兜底填充）。 */
    private String competency;
    /** 题目难度：简单/中等/困难。 */
    private String difficulty;
    /** 题目顺序（从 0 开始）。 */
    private Integer sortOrder;
    /** 来源：QUESTION_BANK / RULE / AI。 */
    private String sourceType;
    private LocalDateTime createdAt;
}
