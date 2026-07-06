package com.example.interviewagent.service;

import com.example.interviewagent.entity.WrongQuestion;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.WrongQuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final UserService userService;
    private final @Lazy QuestionService questionService;

    public void addOrIncrease(Long userId, Long questionId) {
        WrongQuestion wrongQuestion = new WrongQuestion();
        wrongQuestion.setUserId(userId);
        wrongQuestion.setQuestionId(questionId);
        wrongQuestionMapper.insertOrIncrease(wrongQuestion);
    }

    public WrongQuestion create(WrongQuestion wrongQuestion) {
        if (wrongQuestion == null || wrongQuestion.getUserId() == null || wrongQuestion.getQuestionId() == null) {
            throw new BusinessException(400, "用户ID和题目ID不能为空");
        }
        userService.getById(wrongQuestion.getUserId());
        questionService.getById(wrongQuestion.getQuestionId());
        wrongQuestionMapper.insertOrIncrease(wrongQuestion);
        return wrongQuestion;
    }

    public List<WrongQuestion> listByUserId(Long userId) {
        userService.getById(userId);
        return wrongQuestionMapper.selectByUserId(userId);
    }

    public void markMastered(Long id) {
        if (wrongQuestionMapper.markMastered(id) == 0) {
            throw new BusinessException(404, "错题记录不存在");
        }
    }
}
