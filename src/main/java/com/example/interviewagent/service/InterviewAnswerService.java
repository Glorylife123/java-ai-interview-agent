package com.example.interviewagent.service;

import com.example.interviewagent.entity.InterviewAnswer;

import java.util.List;

/**
 * 面试答案基础服务（增查）。
 */
public interface InterviewAnswerService {

    int create(InterviewAnswer answer);

    InterviewAnswer getById(Long id);

    /** 根据题目记录ID查询答案，用于重复提交校验。 */
    InterviewAnswer getByQuestionRecordId(Long questionRecordId);

    /** 查询某会话下的全部答案，用于「查看我的答题情况」。 */
    List<InterviewAnswer> listBySessionId(Long sessionId);
}
