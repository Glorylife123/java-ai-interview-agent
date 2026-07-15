package com.example.interviewagent.interview.orchestrator;

import com.example.interviewagent.dto.InterviewAnswerSubmitDTO;
import com.example.interviewagent.dto.InterviewCreateDTO;
import com.example.interviewagent.entity.InterviewAnswer;
import com.example.interviewagent.entity.InterviewQuestionRecord;
import com.example.interviewagent.entity.InterviewSession;
import com.example.interviewagent.entity.Question;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.interview.evaluator.AnswerEvaluator;
import com.example.interviewagent.interview.generator.QuestionGenerator;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewOrchestratorTests {

    @Mock private InterviewSessionService sessionService;
    @Mock private InterviewQuestionRecordService questionRecordService;
    @Mock private InterviewAnswerService answerService;
    @Mock private InterviewEvaluationService evaluationService;
    @Mock private InterviewReportService reportService;
    @Mock private QuestionMapper questionMapper;
    @Mock private QuestionGenerator questionGenerator;
    @Mock private AnswerEvaluator answerEvaluator;
    @Mock private InterviewReportGenerator reportGenerator;

    private InterviewOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new InterviewOrchestrator(
                sessionService,
                questionRecordService,
                answerService,
                evaluationService,
                reportService,
                questionMapper,
                questionGenerator,
                answerEvaluator,
                reportGenerator,
                new ObjectMapper());
    }

    @Test
    void createSessionStartsInCreatedStateAndCapsQuestionCount() {
        doAnswer(invocation -> {
            InterviewSession session = invocation.getArgument(0);
            session.setId(100L);
            return 1;
        }).when(sessionService).create(any(InterviewSession.class));
        InterviewCreateDTO dto = new InterviewCreateDTO();
        dto.setPosition(" Java 后端实习生 ");
        dto.setDifficulty("中等");
        dto.setTotalQuestionCount(30);

        InterviewSession result = orchestrator.createSession(7L, dto);

        assertEquals(100L, result.getId());
        assertEquals("CREATED", result.getStatus());
        assertEquals("Java 后端实习生", result.getPosition());
        assertEquals(20, result.getTotalQuestionCount());
        assertEquals(0, result.getCurrentQuestionIndex());
    }

    @Test
    void startInterviewMovesSessionToInProgressAndReturnsFirstQuestion() {
        InterviewSession session = session(7L, "CREATED", 5);
        when(sessionService.getById(100L)).thenReturn(session);
        InterviewQuestionRecord record = record(11L, 100L, 21L, 0);
        when(questionGenerator.generateNextQuestion(100L)).thenReturn(record);

        InterviewQuestionVO result = orchestrator.startInterview(7L, 100L);

        assertEquals(11L, result.getQuestionRecordId());
        assertEquals(5, result.getTotalCount());
        ArgumentCaptor<InterviewSession> captor = ArgumentCaptor.forClass(InterviewSession.class);
        verify(sessionService).update(captor.capture());
        assertEquals("IN_PROGRESS", captor.getValue().getStatus());
        assertNotNull(captor.getValue().getStartedAt());
    }

    @Test
    void userCannotAccessAnotherUsersSession() {
        when(sessionService.getById(100L)).thenReturn(session(8L, "IN_PROGRESS", 5));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orchestrator.getCurrentQuestion(7L, 100L));

        assertEquals(403, exception.getCode());
        verify(questionRecordService, never()).listBySessionId(100L);
    }

    @Test
    void duplicateAnswerReturnsConflictBeforeWriting() {
        when(sessionService.getById(100L)).thenReturn(session(7L, "IN_PROGRESS", 1));
        InterviewQuestionRecord record = record(11L, 100L, 21L, 0);
        when(questionRecordService.getById(11L)).thenReturn(record);
        when(answerService.getByQuestionRecordId(11L)).thenReturn(new InterviewAnswer());
        InterviewAnswerSubmitDTO dto = answerDto();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orchestrator.submitAnswer(7L, 100L, dto));

        assertEquals(409, exception.getCode());
        verify(answerService, never()).create(any(InterviewAnswer.class));
        verify(answerEvaluator, never()).evaluate(any());
    }

    @Test
    void lastAnswerFinishesSessionAndGeneratesReport() {
        when(sessionService.getById(100L)).thenReturn(session(7L, "IN_PROGRESS", 1));
        InterviewQuestionRecord record = record(11L, 100L, 21L, 0);
        when(questionRecordService.getById(11L)).thenReturn(record);
        doAnswer(invocation -> {
            InterviewAnswer answer = invocation.getArgument(0);
            answer.setId(99L);
            return 1;
        }).when(answerService).create(any(InterviewAnswer.class));
        Question question = new Question();
        question.setId(21L);
        question.setAnswer("JVM 运行时数据区包含堆和栈");
        when(questionMapper.selectById(21L)).thenReturn(question);
        when(answerEvaluator.evaluate(any())).thenReturn(EvaluationResult.builder()
                .score(75)
                .level("良好")
                .matchedPoints(List.of("堆"))
                .missingPoints(List.of("栈"))
                .suggestion("补充线程私有区域")
                .build());

        InterviewAnswerResultVO result = orchestrator.submitAnswer(7L, 100L, answerDto());

        assertTrue(result.getIsFinished());
        assertEquals(75, result.getScore());
        assertFalse(result.getMatchedPoints().isEmpty());
        verify(evaluationService).create(any());
        verify(sessionService).update(any(InterviewSession.class));
        verify(reportGenerator).generateReport(100L);
    }

    private InterviewSession session(Long userId, String status, int totalQuestionCount) {
        InterviewSession session = new InterviewSession();
        session.setId(100L);
        session.setUserId(userId);
        session.setStatus(status);
        session.setTotalQuestionCount(totalQuestionCount);
        session.setCurrentQuestionIndex(0);
        return session;
    }

    private InterviewQuestionRecord record(Long id, Long sessionId, Long questionId, int sortOrder) {
        InterviewQuestionRecord record = new InterviewQuestionRecord();
        record.setId(id);
        record.setSessionId(sessionId);
        record.setQuestionId(questionId);
        record.setQuestionContent("请说明 JVM 内存模型");
        record.setSortOrder(sortOrder);
        return record;
    }

    private InterviewAnswerSubmitDTO answerDto() {
        InterviewAnswerSubmitDTO dto = new InterviewAnswerSubmitDTO();
        dto.setQuestionRecordId(11L);
        dto.setAnswerContent("JVM 包含堆和栈");
        dto.setDurationSeconds(60);
        return dto;
    }
}
