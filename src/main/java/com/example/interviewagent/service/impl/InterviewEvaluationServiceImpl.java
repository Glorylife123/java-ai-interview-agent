package com.example.interviewagent.service.impl;

import com.example.interviewagent.entity.InterviewEvaluation;
import com.example.interviewagent.mapper.InterviewEvaluationMapper;
import com.example.interviewagent.service.InterviewEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewEvaluationServiceImpl implements InterviewEvaluationService {

    private final InterviewEvaluationMapper interviewEvaluationMapper;

    @Override
    public int create(InterviewEvaluation evaluation) {
        return interviewEvaluationMapper.insert(evaluation);
    }

    @Override
    public InterviewEvaluation getByQuestionRecordId(Long questionRecordId) {
        return interviewEvaluationMapper.selectByQuestionRecordId(questionRecordId);
    }

    @Override
    public List<InterviewEvaluation> listBySessionId(Long sessionId) {
        return interviewEvaluationMapper.selectBySessionId(sessionId);
    }
}
