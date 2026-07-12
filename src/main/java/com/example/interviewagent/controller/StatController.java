package com.example.interviewagent.controller;

import com.example.interviewagent.common.Result;
import com.example.interviewagent.service.AnswerRecordService;
import com.example.interviewagent.vo.CategoryStatVO;
import com.example.interviewagent.vo.DailyStatVO;
import com.example.interviewagent.vo.PracticeStatVO;
import com.example.interviewagent.vo.WeakTagStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 练习统计接口。
 * 全部接口由 JwtAuthInterceptor 保护（/api/** 默认要求 AT），当前登录用户 ID 由拦截器注入 authUserId。
 */
@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
public class StatController {

    private final AnswerRecordService answerRecordService;

    /** 练习总览统计（总答题/正确率/错题本数/平均分）。 */
    @GetMapping("/overview")
    public Result<PracticeStatVO> overview(@RequestAttribute("authUserId") Long userId) {
        return Result.success(answerRecordService.getOverviewStat(userId));
    }

    /** 按分类(tag.category)分组的答题统计。 */
    @GetMapping("/category")
    public Result<List<CategoryStatVO>> category(@RequestAttribute("authUserId") Long userId) {
        return Result.success(answerRecordService.getCategoryStats(userId));
    }

    /** 薄弱标签列表（默认前 5 个错误最多的标签）。 */
    @GetMapping("/weak-tags")
    public Result<List<WeakTagStatVO>> weakTags(
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestAttribute("authUserId") Long userId) {
        return Result.success(answerRecordService.getWeakTagStats(userId, limit));
    }

    /** 每日练习趋势（默认最近 30 天，缺失日期不补 0）。 */
    @GetMapping("/daily")
    public Result<List<DailyStatVO>> daily(
            @RequestParam(defaultValue = "30") Integer days,
            @RequestAttribute("authUserId") Long userId) {
        return Result.success(answerRecordService.getDailyStats(userId, days));
    }
}
