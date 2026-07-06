package com.example.interviewagent.service;

import com.example.interviewagent.entity.UserAnswerRecord;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.UserAnswerRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAnswerRecordService {

    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final UserService userService;
    private final QuestionService questionService;
    private final WrongQuestionService wrongQuestionService;

    public UserAnswerRecord create(UserAnswerRecord record) {
        if (record == null || record.getUserId() == null || record.getQuestionId() == null) {
            throw new BusinessException(400, "用户ID和题目ID不能为空");
        }
        userService.getById(record.getUserId());
        questionService.getById(record.getQuestionId());
        if (!StringUtils.hasText(record.getAnswerSource())) {
            record.setAnswerSource("PRACTICE");
        }
        userAnswerRecordMapper.insert(record);
        questionService.increaseSubmitCount(record.getQuestionId(), record.getIsCorrect());
        if (Integer.valueOf(0).equals(record.getIsCorrect())) {
            wrongQuestionService.addOrIncrease(record.getUserId(), record.getQuestionId());
        }
        return record;
    }

    public List<UserAnswerRecord> listByUserId(Long userId) {
        userService.getById(userId);
        return userAnswerRecordMapper.selectByUserId(userId);
    }
}
