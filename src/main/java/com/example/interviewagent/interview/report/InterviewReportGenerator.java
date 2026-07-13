package com.example.interviewagent.interview.report;

import com.example.interviewagent.entity.InterviewReport;

/**
 * 报告生成策略接口。
 */
public interface InterviewReportGenerator {

    /**
     * 生成面试报告（已保存到数据库；若已存在则先删后插）。
     *
     * @param sessionId 会话ID
     * @return 生成的报告实体
     */
    InterviewReport generateReport(Long sessionId);
}
