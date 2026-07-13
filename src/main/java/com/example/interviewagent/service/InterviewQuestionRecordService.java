package com.example.interviewagent.service;

import com.example.interviewagent.entity.InterviewQuestionRecord;

import java.util.List;

/**
 * 面试题目记录基础服务（增查）。
 */
public interface InterviewQuestionRecordService {

    int create(InterviewQuestionRecord record);

    InterviewQuestionRecord getById(Long id);

    List<InterviewQuestionRecord> listBySessionId(Long sessionId);

    int countBySessionId(Long sessionId);

    InterviewQuestionRecord getBySessionIdAndOrder(Long sessionId, Integer sortOrder);
}
