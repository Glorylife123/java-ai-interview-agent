package com.example.interviewagent.interview.model;

import com.example.interviewagent.entity.InterviewQuestionRecord;
import com.example.interviewagent.entity.InterviewSession;
import com.example.interviewagent.entity.Question;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评分上下文：承载评分所需的全部输入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationContext {

    private InterviewSession session;
    private InterviewQuestionRecord questionRecord;
    /** 原始题库题目（含标准答案与解析）。 */
    private Question question;
    private String userAnswer;
    private Integer durationSeconds;
}
