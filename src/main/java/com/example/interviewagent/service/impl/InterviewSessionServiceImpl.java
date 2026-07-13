package com.example.interviewagent.service.impl;

import com.example.interviewagent.entity.InterviewSession;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.InterviewSessionMapper;
import com.example.interviewagent.service.InterviewSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private final InterviewSessionMapper interviewSessionMapper;

    @Override
    public int create(InterviewSession session) {
        return interviewSessionMapper.insert(session);
    }

    @Override
    public InterviewSession getById(Long id) {
        if (id == null) {
            throw new BusinessException(400, "会话ID不能为空");
        }
        InterviewSession session = interviewSessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(404, "面试会话不存在");
        }
        return session;
    }

    @Override
    public int update(InterviewSession session) {
        return interviewSessionMapper.update(session);
    }

    @Override
    public List<InterviewSession> listByUserId(Long userId) {
        return interviewSessionMapper.selectByUserId(userId);
    }

    @Override
    public int countByUserId(Long userId) {
        return interviewSessionMapper.countByUserId(userId);
    }
}
