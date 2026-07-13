package com.example.interviewagent.service.impl;

import com.example.interviewagent.entity.InterviewQuestionRecord;
import com.example.interviewagent.mapper.InterviewQuestionRecordMapper;
import com.example.interviewagent.service.InterviewQuestionRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewQuestionRecordServiceImpl implements InterviewQuestionRecordService {

    private final InterviewQuestionRecordMapper interviewQuestionRecordMapper;

    @Override
    public int create(InterviewQuestionRecord record) {
        return interviewQuestionRecordMapper.insert(record);
    }

    @Override
    public InterviewQuestionRecord getById(Long id) {
        return interviewQuestionRecordMapper.selectById(id);
    }

    @Override
    public List<InterviewQuestionRecord> listBySessionId(Long sessionId) {
        return interviewQuestionRecordMapper.selectBySessionId(sessionId);
    }

    @Override
    public int countBySessionId(Long sessionId) {
        return interviewQuestionRecordMapper.countBySessionId(sessionId);
    }

    @Override
    public InterviewQuestionRecord getBySessionIdAndOrder(Long sessionId, Integer sortOrder) {
        return interviewQuestionRecordMapper.selectBySessionIdAndOrder(sessionId, sortOrder);
    }
}
