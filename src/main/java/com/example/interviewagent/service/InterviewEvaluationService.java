package com.example.interviewagent.service;

import com.example.interviewagent.entity.InterviewEvaluation;

import java.util.List;

/**
 * 面试评分基础服务（增查）。
 */
public interface InterviewEvaluationService {

    int create(InterviewEvaluation evaluation);

    InterviewEvaluation getByQuestionRecordId(Long questionRecordId);

    List<InterviewEvaluation> listBySessionId(Long sessionId);
}
