package com.example.interviewagent.interview.orchestrator;

import com.example.interviewagent.dto.InterviewAnswerSubmitDTO;
import com.example.interviewagent.dto.InterviewCreateDTO;
import com.example.interviewagent.entity.InterviewAnswer;
import com.example.interviewagent.entity.InterviewEvaluation;
import com.example.interviewagent.entity.InterviewQuestionRecord;
import com.example.interviewagent.entity.InterviewReport;
import com.example.interviewagent.entity.InterviewSession;
import com.example.interviewagent.entity.Question;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.interview.InterviewDifficulty;
import com.example.interviewagent.interview.evaluator.AnswerEvaluator;
import com.example.interviewagent.interview.generator.QuestionGenerator;
import com.example.interviewagent.interview.model.EvaluationContext;
import com.example.interviewagent.interview.model.EvaluationResult;
import com.example.interviewagent.interview.report.InterviewReportGenerator;
import com.example.interviewagent.mapper.QuestionMapper;
import com.example.interviewagent.service.InterviewAnswerService;
import com.example.interviewagent.service.InterviewEvaluationService;
import com.example.interviewagent.service.InterviewQuestionRecordService;
import com.example.interviewagent.service.InterviewReportService;
import com.example.interviewagent.service.InterviewSessionService;
import com.example.interviewagent.vo.InterviewAnswerResultVO;
import com.example.interviewagent.vo.InterviewQuestionVO;
import com.example.interviewagent.vo.InterviewReportVO;
import com.example.interviewagent.vo.InterviewSessionVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试流程编排器：串联出题、评分、报告生成，供 Controller 调用。
 * 所有会写多表的方法均加事务，保证原子性。
 * <p>
 * 状态流转：CREATED --start--> IN_PROGRESS --答完最后一题--> FINISHED。
 */
@Component
@RequiredArgsConstructor
public class InterviewOrchestrator {

    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_FINISHED = "FINISHED";

    private static final int DEFAULT_QUESTION_COUNT = 5;
    private static final int MAX_QUESTION_COUNT = 20;

    private final InterviewSessionService interviewSessionService;
    private final InterviewQuestionRecordService interviewQuestionRecordService;
    private final InterviewAnswerService interviewAnswerService;
    private final InterviewEvaluationService interviewEvaluationService;
    private final InterviewReportService interviewReportService;
    private final QuestionMapper questionMapper;

    private final QuestionGenerator questionGenerator;
    private final AnswerEvaluator answerEvaluator;
    private final InterviewReportGenerator interviewReportGenerator;
    private final ObjectMapper objectMapper;

    /** 创建会话（状态 CREATED）。 */
    @Transactional
    public InterviewSession createSession(Long userId, InterviewCreateDTO dto) {
        requireLogin(userId);
        if (dto == null || !StringUtils.hasText(dto.getPosition()) || !StringUtils.hasText(dto.getDifficulty())) {
            throw new BusinessException(400, "岗位和难度不能为空");
        }
        if (!InterviewDifficulty.isValid(dto.getDifficulty())) {
            throw new BusinessException(400, "难度仅支持：简单、中等、困难");
        }
        int total = normalizeQuestionCount(dto.getTotalQuestionCount());

        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setPosition(dto.getPosition().trim());
        session.setTitle(dto.getPosition().trim() + "模拟面试");
        session.setDifficulty(dto.getDifficulty().trim());
        session.setStatus(STATUS_CREATED);
        session.setTotalQuestionCount(total);
        session.setCurrentQuestionIndex(0);
        interviewSessionService.create(session);
        return session;
    }

    /** 开始面试：校验归属与状态，置 IN_PROGRESS，生成第一题。 */
    @Transactional
    public InterviewQuestionVO startInterview(Long userId, Long sessionId) {
        InterviewSession session = loadOwnedSession(userId, sessionId);
        if (!STATUS_CREATED.equals(session.getStatus())) {
            throw new BusinessException(409, "面试已开始或已结束，无法重复开始");
        }

        InterviewSession update = new InterviewSession();
        update.setId(sessionId);
        update.setStatus(STATUS_IN_PROGRESS);
        update.setStartedAt(LocalDateTime.now());
        interviewSessionService.update(update);

        InterviewQuestionRecord record = questionGenerator.generateNextQuestion(sessionId);
        return toQuestionVO(record, session.getTotalQuestionCount());
    }

    /** 获取当前题目（不推进流程）：返回本场最后一道已出的题。 */
    @Transactional(readOnly = true)
    public InterviewQuestionVO getCurrentQuestion(Long userId, Long sessionId) {
        InterviewSession session = loadOwnedSession(userId, sessionId);
        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
            throw new BusinessException(409, "面试尚未开始或已结束");
        }
        List<InterviewQuestionRecord> records = interviewQuestionRecordService.listBySessionId(sessionId);
        if (records.isEmpty()) {
            throw new BusinessException(404, "当前没有可作答的题目");
        }
        InterviewQuestionRecord current = records.get(records.size() - 1);
        return toQuestionVO(current, session.getTotalQuestionCount());
    }

    /** 提交回答：保存答案、评分，推进或结束面试。 */
    @Transactional
    public InterviewAnswerResultVO submitAnswer(Long userId, Long sessionId, InterviewAnswerSubmitDTO dto) {
        InterviewSession session = loadOwnedSession(userId, sessionId);
        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
            throw new BusinessException(409, "面试尚未开始或已结束，无法提交回答");
        }
        if (dto == null || dto.getQuestionRecordId() == null || !StringUtils.hasText(dto.getAnswerContent())) {
            throw new BusinessException(400, "题目记录ID和回答内容不能为空");
        }

        InterviewQuestionRecord record = interviewQuestionRecordService.getById(dto.getQuestionRecordId());
        if (record == null || !record.getSessionId().equals(sessionId)) {
            throw new BusinessException(404, "题目记录不存在或不属于本次面试");
        }
        // 防止重复提交
        if (interviewAnswerService.getByQuestionRecordId(record.getId()) != null) {
            throw new BusinessException(409, "该题目已作答，请勿重复提交");
        }

        // 保存答案
        InterviewAnswer answer = new InterviewAnswer();
        answer.setSessionId(sessionId);
        answer.setQuestionRecordId(record.getId());
        answer.setUserId(userId);
        answer.setAnswerContent(dto.getAnswerContent());
        answer.setDurationSeconds(dto.getDurationSeconds());
        interviewAnswerService.create(answer);

        // 评分（规则版）
        Question question = questionMapper.selectById(record.getQuestionId());
        EvaluationContext context = EvaluationContext.builder()
                .session(session)
                .questionRecord(record)
                .question(question)
                .userAnswer(dto.getAnswerContent())
                .durationSeconds(dto.getDurationSeconds())
                .build();
        EvaluationResult result = answerEvaluator.evaluate(context);

        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setSessionId(sessionId);
        evaluation.setQuestionRecordId(record.getId());
        evaluation.setAnswerId(answer.getId());
        evaluation.setScore(result.getScore());
        evaluation.setMaxScore(100);
        evaluation.setLevel(result.getLevel());
        evaluation.setMatchedPoints(joinPoints(result.getMatchedPoints()));
        evaluation.setMissingPoints(joinPoints(result.getMissingPoints()));
        evaluation.setSuggestion(result.getSuggestion());
        evaluation.setEvaluatorType("RULE");
        interviewEvaluationService.create(evaluation);

        InterviewAnswerResultVO vo = new InterviewAnswerResultVO();
        vo.setQuestionRecordId(record.getId());
        vo.setScore(result.getScore());
        vo.setLevel(result.getLevel());
        vo.setMatchedPoints(result.getMatchedPoints());
        vo.setMissingPoints(result.getMissingPoints());
        vo.setSuggestion(result.getSuggestion());

        // 是否还有下一题：出题会推进 currentQuestionIndex，此处以已出题数判断更稳妥。
        int answeredCount = record.getSortOrder() + 1;
        boolean hasNext = answeredCount < session.getTotalQuestionCount();
        if (hasNext) {
            InterviewQuestionRecord next = questionGenerator.generateNextQuestion(sessionId);
            vo.setIsFinished(false);
            vo.setNextQuestion(toQuestionVO(next, session.getTotalQuestionCount()));
        } else {
            InterviewSession finish = new InterviewSession();
            finish.setId(sessionId);
            finish.setStatus(STATUS_FINISHED);
            finish.setEndedAt(LocalDateTime.now());
            interviewSessionService.update(finish);
            interviewReportGenerator.generateReport(sessionId);
            vo.setIsFinished(true);
            vo.setNextQuestion(null);
        }
        return vo;
    }

    /** 获取面试报告（直接查询，不重新生成）。 */
    @Transactional(readOnly = true)
    public InterviewReportVO getReport(Long userId, Long sessionId) {
        loadOwnedSession(userId, sessionId);
        InterviewReport report = interviewReportService.getBySessionId(sessionId);
        if (report == null) {
            throw new BusinessException(404, "面试报告不存在，请先完成面试");
        }
        InterviewReportVO vo = new InterviewReportVO();
        vo.setSessionId(report.getSessionId());
        vo.setOverallScore(report.getOverallScore());
        vo.setSummary(report.getSummary());
        vo.setDimensionScores(parseDimensionScores(report.getDimensionScoresJson()));
        vo.setSuggestions(parseSuggestions(report.getSuggestionsJson()));
        vo.setGeneratorType(report.getGeneratorType());
        vo.setCreatedAt(report.getCreatedAt());
        return vo;
    }

    /** 列出当前用户的全部面试历史（会话摘要，按创建时间倒序）。 */
    @Transactional(readOnly = true)
    public List<InterviewSessionVO> listSessions(Long userId) {
        requireLogin(userId);
        List<InterviewSession> sessions = interviewSessionService.listByUserId(userId);
        List<InterviewSessionVO> result = new ArrayList<>(sessions.size());
        for (InterviewSession s : sessions) {
            InterviewSessionVO vo = new InterviewSessionVO();
            vo.setId(s.getId());
            vo.setTitle(s.getTitle());
            vo.setPosition(s.getPosition());
            vo.setDifficulty(s.getDifficulty());
            vo.setStatus(s.getStatus());
            vo.setTotalQuestionCount(s.getTotalQuestionCount());
            vo.setCurrentQuestionIndex(s.getCurrentQuestionIndex());
            vo.setStartedAt(s.getStartedAt());
            vo.setEndedAt(s.getEndedAt());
            result.add(vo);
        }
        return result;
    }

    // ==================== 私有工具 ====================

    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
    }

    /** 加载会话并校验归属当前用户。 */
    private InterviewSession loadOwnedSession(Long userId, Long sessionId) {
        requireLogin(userId);
        InterviewSession session = interviewSessionService.getById(sessionId);
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该面试会话");
        }
        return session;
    }

    private int normalizeQuestionCount(Integer count) {
        if (count == null || count < 1) {
            return DEFAULT_QUESTION_COUNT;
        }
        return Math.min(count, MAX_QUESTION_COUNT);
    }

    private InterviewQuestionVO toQuestionVO(InterviewQuestionRecord record, Integer totalCount) {
        InterviewQuestionVO vo = new InterviewQuestionVO();
        vo.setQuestionRecordId(record.getId());
        vo.setQuestionId(record.getQuestionId());
        vo.setQuestionContent(record.getQuestionContent());
        vo.setSortOrder(record.getSortOrder());
        vo.setTotalCount(totalCount);
        return vo;
    }

    /** 关键句以换行连接持久化（关键句本身不含换行，报告端按换行还原）。 */
    private String joinPoints(List<String> points) {
        if (points == null || points.isEmpty()) {
            return "";
        }
        return String.join("\n", points);
    }

    /** 反序列化维度得分 JSON；异常或空时返回空 Map，不阻断报告展示。 */
    private Map<String, Integer> parseDimensionScores(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Integer>>() {
            });
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }

    /** 反序列化学习建议 JSON；异常或空时返回空列表。 */
    private List<String> parseSuggestions(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<ArrayList<String>>() {
            });
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
