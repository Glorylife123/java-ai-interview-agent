package com.example.interviewagent.service;

import com.example.interviewagent.entity.InterviewReport;

/**
 * 面试报告基础服务（增查删）。报告生成规则见 RuleBasedInterviewReportGenerator。
 */
public interface InterviewReportService {

    int create(InterviewReport report);

    InterviewReport getBySessionId(Long sessionId);

    int deleteBySessionId(Long sessionId);
}
