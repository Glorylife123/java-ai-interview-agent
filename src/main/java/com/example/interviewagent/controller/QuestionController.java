package com.example.interviewagent.controller;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.common.Result;
import com.example.interviewagent.entity.Question;
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
    public Result<Question> create(@RequestBody Question question) {
        return Result.success(questionService.create(question));
    }

    @GetMapping("/{id}")
    public Result<Question> getById(@PathVariable Long id) {
        return Result.success(questionService.viewDetail(id));
    }

    @GetMapping
    public Result<PageResult<Question>> page(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer difficulty,
                                             @RequestParam(required = false) Long tagId,
                                             @RequestParam(required = false) Integer pageNum,
                                             @RequestParam(required = false) Integer pageSize) {
        return Result.success(questionService.page(keyword, difficulty, tagId, pageNum, pageSize));
    }

    @PutMapping("/{id}")
    public Result<Question> update(@PathVariable Long id, @RequestBody Question question) {
        return Result.success(questionService.update(id, question));
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
