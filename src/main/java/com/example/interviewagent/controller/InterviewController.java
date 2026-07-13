package com.example.interviewagent.controller;

import com.example.interviewagent.common.Result;
import com.example.interviewagent.dto.InterviewAnswerSubmitDTO;
import com.example.interviewagent.dto.InterviewCreateDTO;
import com.example.interviewagent.entity.InterviewSession;
import com.example.interviewagent.interview.orchestrator.InterviewOrchestrator;
import com.example.interviewagent.vo.InterviewAnswerResultVO;
import com.example.interviewagent.vo.InterviewQuestionVO;
import com.example.interviewagent.vo.InterviewReportVO;
import com.example.interviewagent.vo.InterviewSessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 规则版模拟面试接口（本迭代不接入 AI）。
 * 当前登录用户 ID 由认证拦截器写入请求属性 authUserId，前端无需传递。
 */
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewOrchestrator interviewOrchestrator;

    /** 创建面试会话（状态 CREATED）。 */
    @PostMapping("/session")
    public Result<InterviewSessionVO> createSession(@RequestBody @Valid InterviewCreateDTO dto,
                                                    @RequestAttribute("authUserId") Long userId) {
        InterviewSession session = interviewOrchestrator.createSession(userId, dto);
        return Result.success(toSessionVO(session));
    }

    /** 开始面试，返回第一题。 */
    @PostMapping("/session/{sessionId}/start")
    public Result<InterviewQuestionVO> start(@PathVariable Long sessionId,
                                             @RequestAttribute("authUserId") Long userId) {
        return Result.success(interviewOrchestrator.startInterview(userId, sessionId));
    }

    /** 获取当前题目（不推进流程）。 */
    @GetMapping("/session/{sessionId}/current-question")
    public Result<InterviewQuestionVO> currentQuestion(@PathVariable Long sessionId,
                                                       @RequestAttribute("authUserId") Long userId) {
        return Result.success(interviewOrchestrator.getCurrentQuestion(userId, sessionId));
    }

    /** 提交回答，返回本次评分与下一题（或结束标记）。 */
    @PostMapping("/session/{sessionId}/answer")
    public Result<InterviewAnswerResultVO> answer(@PathVariable Long sessionId,
                                                  @RequestBody @Valid InterviewAnswerSubmitDTO dto,
                                                  @RequestAttribute("authUserId") Long userId) {
        return Result.success(interviewOrchestrator.submitAnswer(userId, sessionId, dto));
    }

    /** 获取面试报告。 */
    @GetMapping("/session/{sessionId}/report")
    public Result<InterviewReportVO> report(@PathVariable Long sessionId,
                                            @RequestAttribute("authUserId") Long userId) {
        return Result.success(interviewOrchestrator.getReport(userId, sessionId));
    }

    /** 列出当前用户的全部面试历史。 */
    @GetMapping("/sessions")
    public Result<List<InterviewSessionVO>> sessions(@RequestAttribute("authUserId") Long userId) {
        return Result.success(interviewOrchestrator.listSessions(userId));
    }

    private InterviewSessionVO toSessionVO(InterviewSession session) {
        InterviewSessionVO vo = new InterviewSessionVO();
        vo.setId(session.getId());
        vo.setTitle(session.getTitle());
        vo.setPosition(session.getPosition());
        vo.setDifficulty(session.getDifficulty());
        vo.setStatus(session.getStatus());
        vo.setTotalQuestionCount(session.getTotalQuestionCount());
        vo.setCurrentQuestionIndex(session.getCurrentQuestionIndex());
        vo.setStartedAt(session.getStartedAt());
        vo.setEndedAt(session.getEndedAt());
        return vo;
    }
}
