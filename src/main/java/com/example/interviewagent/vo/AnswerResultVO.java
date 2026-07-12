package com.example.interviewagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交答案后立即返回的结果：回显用户答案，并附带标准答案与解析供用户自评。
 * 本迭代不做正误判断，因此不含 isCorrect / score 字段。
 */
@Data
public class AnswerResultVO {

    private Long questionId;
    private String questionTitle;
    private String userAnswer;
    /** 标准答案，取自 Question.answer。 */
    private String standardAnswer;
    /** 题目解析，取自 Question.answerAnalysis。 */
    private String analysis;
    private LocalDateTime submitTime;
}
