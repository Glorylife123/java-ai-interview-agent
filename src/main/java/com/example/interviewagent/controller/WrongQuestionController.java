package com.example.interviewagent.controller;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.common.Result;
import com.example.interviewagent.controller.dto.QuestionResponse;
import com.example.interviewagent.dto.WrongQuestionQueryDTO;
import com.example.interviewagent.service.WrongQuestionService;
import com.example.interviewagent.vo.WrongQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 错题本接口。
 * <ul>
 *   <li>/api/wrong/** 使用 @RequestAttribute("authUserId") 获取当前用户；</li>
 *   <li>所有读写均以当前登录用户为边界，不接收客户端提供的 userId。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/wrong")
@RequiredArgsConstructor
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    /** 分页查询错题列表，带回题目信息。 */
    @GetMapping("/page")
    public Result<PageResult<WrongQuestionVO>> page(WrongQuestionQueryDTO query,
                                                     @RequestAttribute("authUserId") Long userId) {
        return Result.success(wrongQuestionService.pageWrongQuestions(userId, query));
    }

    /** 移除错题（物理删除）。 */
    @DeleteMapping("/{questionId}")
    public Result<Void> remove(@PathVariable Long questionId,
                               @RequestAttribute("authUserId") Long userId) {
        wrongQuestionService.removeWrongQuestion(userId, questionId);
        return Result.success();
    }

    /** 标记已掌握 / 取消掌握。请求体示例：{"mastered": true}。 */
    @PutMapping("/{questionId}/mastered")
    public Result<Void> mastered(@PathVariable Long questionId,
                                 @RequestBody(required = false) Map<String, Boolean> body,
                                 @RequestAttribute("authUserId") Long userId) {
        Boolean mastered = body != null ? body.get("mastered") : null;
        wrongQuestionService.markMastered(userId, questionId, mastered);
        return Result.success();
    }

    /** 随机获取一道错题的完整详情（含标签），无错题时返回 data = null。 */
    @GetMapping("/random")
    public Result<QuestionResponse> random(@RequestAttribute("authUserId") Long userId) {
        QuestionResponse detail = wrongQuestionService.getRandomWrongQuestionDetail(userId);
        return Result.success(detail);
    }
}
