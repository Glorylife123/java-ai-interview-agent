package com.example.interviewagent.service.impl;

import com.example.interviewagent.entity.InterviewReport;
import com.example.interviewagent.mapper.InterviewReportMapper;
import com.example.interviewagent.service.InterviewReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterviewReportServiceImpl implements InterviewReportService {

    private final InterviewReportMapper interviewReportMapper;

    @Override
    public int create(InterviewReport report) {
        return interviewReportMapper.insert(report);
    }

    @Override
    public InterviewReport getBySessionId(Long sessionId) {
        return interviewReportMapper.selectBySessionId(sessionId);
    }

    @Override
    public int deleteBySessionId(Long sessionId) {
        return interviewReportMapper.deleteBySessionId(sessionId);
    }
}
