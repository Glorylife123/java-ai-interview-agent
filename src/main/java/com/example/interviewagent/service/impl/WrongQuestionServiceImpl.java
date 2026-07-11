package com.example.interviewagent.service.impl;

import com.example.interviewagent.entity.WrongQuestion;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.WrongQuestionMapper;
import com.example.interviewagent.service.QuestionService;
import com.example.interviewagent.service.UserService;
import com.example.interviewagent.service.WrongQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WrongQuestionServiceImpl implements WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final UserService userService;
    private final @Lazy QuestionService questionService;

    @Override
    public void addOrIncrease(Long userId, Long questionId) {
        WrongQuestion wrongQuestion = new WrongQuestion();
        wrongQuestion.setUserId(userId);
        wrongQuestion.setQuestionId(questionId);
        wrongQuestionMapper.insertOrIncrease(wrongQuestion);
    }

    @Override
    public WrongQuestion create(WrongQuestion wrongQuestion) {
        if (wrongQuestion == null || wrongQuestion.getUserId() == null || wrongQuestion.getQuestionId() == null) {
            throw new BusinessException(400, "用户ID和题目ID不能为空");
        }
        userService.getById(wrongQuestion.getUserId());
        questionService.getById(wrongQuestion.getQuestionId());
        wrongQuestionMapper.insertOrIncrease(wrongQuestion);
        return wrongQuestion;
    }

    @Override
    public List<WrongQuestion> listByUserId(Long userId) {
        userService.getById(userId);
        return wrongQuestionMapper.selectByUserId(userId);
    }

    @Override
    public void markMastered(Long id) {
        if (wrongQuestionMapper.markMastered(id) == 0) {
            throw new BusinessException(404, "错题记录不存在");
        }
    }
}
