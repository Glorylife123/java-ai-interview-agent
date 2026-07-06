package com.example.interviewagent.controller;

import com.example.interviewagent.common.Result;
import com.example.interviewagent.entity.UserAnswerRecord;
import com.example.interviewagent.service.UserAnswerRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/answer-records")
@RequiredArgsConstructor
public class UserAnswerRecordController {

    private final UserAnswerRecordService userAnswerRecordService;

    @PostMapping
    public Result<UserAnswerRecord> create(@RequestBody UserAnswerRecord record) {
        return Result.success(userAnswerRecordService.create(record));
    }

    @GetMapping("/users/{userId}")
    public Result<List<UserAnswerRecord>> listByUserId(@PathVariable Long userId) {
        return Result.success(userAnswerRecordService.listByUserId(userId));
    }
}
