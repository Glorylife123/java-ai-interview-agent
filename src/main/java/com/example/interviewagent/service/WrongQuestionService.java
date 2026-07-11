package com.example.interviewagent.service;

import com.example.interviewagent.entity.WrongQuestion;

import java.util.List;

public interface WrongQuestionService {

    void addOrIncrease(Long userId, Long questionId);

    WrongQuestion create(WrongQuestion wrongQuestion);

    List<WrongQuestion> listByUserId(Long userId);

    void markMastered(Long id);
}
