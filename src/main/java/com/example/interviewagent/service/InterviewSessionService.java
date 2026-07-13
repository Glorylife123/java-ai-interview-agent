package com.example.interviewagent.service;

import com.example.interviewagent.entity.InterviewSession;

import java.util.List;

/**
 * 面试会话基础服务（增改查）。核心面试编排逻辑见 InterviewOrchestrator。
 */
public interface InterviewSessionService {

    int create(InterviewSession session);

    /** 按 ID 查询，不存在时抛 404。 */
    InterviewSession getById(Long id);

    int update(InterviewSession session);

    List<InterviewSession> listByUserId(Long userId);

    int countByUserId(Long userId);
}
