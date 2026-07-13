package com.example.interviewagent.interview.evaluator;

import com.example.interviewagent.interview.model.EvaluationContext;
import com.example.interviewagent.interview.model.EvaluationResult;

/**
 * 评分策略接口。
 */
public interface AnswerEvaluator {

    /**
     * 对用户的回答进行评分。
     *
     * @param context 包含会话、题目记录、原始题目、用户答案等信息
     * @return 评估结果
     */
    EvaluationResult evaluate(EvaluationContext context);
}
