package com.example.interviewagent.service;

import com.example.interviewagent.entity.UserAnswerRecord;

import java.util.List;

public interface UserAnswerRecordService {

    UserAnswerRecord create(UserAnswerRecord record);

    List<UserAnswerRecord> listByUserId(Long userId);
}
