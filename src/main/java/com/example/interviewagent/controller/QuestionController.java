package com.example.interviewagent.controller;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.common.Result;
import com.example.interviewagent.controller.dto.QuestionResponse;
import com.example.interviewagent.controller.dto.QuestionUpsertRequest;
import com.example.interviewagent.entity.Tag;
import com.example.interviewagent.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public Result<QuestionResponse> create(@RequestBody QuestionUpsertRequest request) {
        return Result.success(questionService.create(request));
    }

    @GetMapping("/{id}")
    public Result<QuestionResponse> getById(@PathVariable Long id) {
        return Result.success(questionService.viewDetail(id));
    }

    @GetMapping
    public Result<PageResult<QuestionResponse>> page(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer difficulty,
                                                     @RequestParam(required = false) String questionType,
                                                     @RequestParam(required = false) Long tagId,
                                                     @RequestParam(required = false) Integer pageNum,
                                                     @RequestParam(required = false) Integer pageSize) {
        return Result.success(questionService.page(keyword, difficulty, questionType, tagId, pageNum, pageSize));
    }

    @PutMapping("/{id}")
    public Result<QuestionResponse> update(@PathVariable Long id, @RequestBody QuestionUpsertRequest request) {
        return Result.success(questionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/tags/{tagId}")
    public Result<Void> bindTag(@PathVariable Long id, @PathVariable Long tagId) {
        questionService.bindTag(id, tagId);
        return Result.success();
    }

    @GetMapping("/{id}/tags")
    public Result<List<Tag>> listTags(@PathVariable Long id) {
        return Result.success(questionService.listTags(id));
    }
}
