package com.example.interviewagent.controller;

import com.example.interviewagent.common.Result;
import com.example.interviewagent.entity.WrongQuestion;
import com.example.interviewagent.service.WrongQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wrong-questions")
@RequiredArgsConstructor
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    @PostMapping
    public Result<WrongQuestion> create(@RequestBody WrongQuestion wrongQuestion) {
        return Result.success(wrongQuestionService.create(wrongQuestion));
    }

    @GetMapping("/users/{userId}")
    public Result<List<WrongQuestion>> listByUserId(@PathVariable Long userId) {
        return Result.success(wrongQuestionService.listByUserId(userId));
    }

    @PutMapping("/{id}/mastered")
    public Result<Void> markMastered(@PathVariable Long id) {
        wrongQuestionService.markMastered(id);
        return Result.success();
    }
}
