package com.example.interviewagent.interview.generator;

import com.example.interviewagent.entity.InterviewQuestionRecord;

/**
 * 出题策略接口。
 */
public interface QuestionGenerator {

    /**
     * 为指定面试会话生成下一题（已保存到数据库，并推进会话的 currentQuestionIndex）。
     *
     * @param sessionId 会话ID
     * @return 生成的题目记录
     */
    InterviewQuestionRecord generateNextQuestion(Long sessionId);
}
