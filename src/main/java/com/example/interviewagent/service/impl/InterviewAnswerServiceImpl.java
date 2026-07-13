package com.example.interviewagent.service.impl;

import com.example.interviewagent.entity.InterviewAnswer;
import com.example.interviewagent.mapper.InterviewAnswerMapper;
import com.example.interviewagent.service.InterviewAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterviewAnswerServiceImpl implements InterviewAnswerService {

    private final InterviewAnswerMapper interviewAnswerMapper;

    @Override
    public int create(InterviewAnswer answer) {
        return interviewAnswerMapper.insert(answer);
    }

    @Override
    public InterviewAnswer getById(Long id) {
        return interviewAnswerMapper.selectById(id);
    }

    @Override
    public InterviewAnswer getByQuestionRecordId(Long questionRecordId) {
        return interviewAnswerMapper.selectByQuestionRecordId(questionRecordId);
    }
}
