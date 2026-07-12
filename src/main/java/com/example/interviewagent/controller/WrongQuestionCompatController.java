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

/**
 * 错题本旧版兼容接口（路径 /api/wrong-questions/**）。
 * <p>新业务请使用 /api/wrong/**（{@link WrongQuestionController}），
 * 本控制器仅保留迁移过渡期向后兼容。</p>
 */
@RestController
@RequestMapping("/api/wrong-questions")
@RequiredArgsConstructor
public class WrongQuestionCompatController {

    private final WrongQuestionService wrongQuestionService;

    /** 新增或累加错题（旧接口，保留兼容）。 */
    @PostMapping
    public Result<WrongQuestion> create(@RequestBody WrongQuestion wrongQuestion) {
        return Result.success(wrongQuestionService.create(wrongQuestion));
    }

    /** 查询用户错题列表（旧接口）。 */
    @GetMapping("/users/{userId}")
    public Result<List<WrongQuestion>> listByUserId(@PathVariable Long userId) {
        return Result.success(wrongQuestionService.listByUserId(userId));
    }

    /** 按主键标记已掌握（旧接口）。 */
    @PutMapping("/{id}/mastered")
    public Result<Void> markMastered(@PathVariable Long id) {
        wrongQuestionService.markMastered(id);
        return Result.success();
    }
}
