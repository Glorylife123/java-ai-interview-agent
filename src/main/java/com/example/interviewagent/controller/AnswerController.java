package com.example.interviewagent.controller;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.common.Result;
import com.example.interviewagent.dto.AnswerSubmitDTO;
import com.example.interviewagent.service.AnswerRecordService;
import com.example.interviewagent.vo.AnswerResultVO;
import com.example.interviewagent.vo.AnswerRecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 答题记录接口（本迭代不接入 AI 判断）。
 * 当前登录用户 ID 由认证拦截器写入请求属性 authUserId，前端无需传递。
 */
@RestController
@RequestMapping("/api/answer")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerRecordService answerRecordService;

    /** 提交答案：保存用户答案并回显标准答案与解析。 */
    @PostMapping("/submit")
    public Result<AnswerResultVO> submit(@RequestBody @Valid AnswerSubmitDTO dto,
                                         @RequestAttribute("authUserId") Long userId) {
        return Result.success(answerRecordService.submitAnswer(userId, dto));
    }

    /** 分页查询当前用户的历史答题记录。 */
    @GetMapping("/records")
    public Result<PageResult<AnswerRecordVO>> records(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestAttribute("authUserId") Long userId) {
        return Result.success(answerRecordService.pageRecords(userId, pageNum, pageSize));
    }
}
