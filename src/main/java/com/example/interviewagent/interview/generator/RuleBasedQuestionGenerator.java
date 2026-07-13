package com.example.interviewagent.interview.generator;

import com.example.interviewagent.entity.InterviewQuestionRecord;
import com.example.interviewagent.entity.InterviewSession;
import com.example.interviewagent.entity.Question;
import com.example.interviewagent.interview.InterviewDifficulty;
import com.example.interviewagent.mapper.QuestionMapper;
import com.example.interviewagent.service.InterviewQuestionRecordService;
import com.example.interviewagent.service.InterviewSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 规则版出题实现：从题库随机抽取符合难度、且本场未出过的上架题目。
 * 第一版忽略 position（岗位）语义，只按 difficulty 过滤 + 随机抽取。
 */
@Component
@RequiredArgsConstructor
public class RuleBasedQuestionGenerator implements QuestionGenerator {

    private final InterviewSessionService interviewSessionService;
    private final InterviewQuestionRecordService interviewQuestionRecordService;
    private final QuestionMapper questionMapper;

    @Override
    public InterviewQuestionRecord generateNextQuestion(Long sessionId) {
        InterviewSession session = interviewSessionService.getById(sessionId);

        // 已出过的题目 ID，避免重复出题
        List<InterviewQuestionRecord> existing = interviewQuestionRecordService.listBySessionId(sessionId);
        List<Long> excludeIds = existing.stream()
                .map(InterviewQuestionRecord::getQuestionId)
                .toList();

        Integer questionDifficulty = InterviewDifficulty.toQuestionDifficulty(session.getDifficulty());
        Question question = questionMapper.selectRandomForInterview(questionDifficulty, excludeIds);
        if (question == null) {
            // 题库中已无符合条件（指定难度、本场未出过、已上架）的题目。
            // 这里返回 null 而非抛异常：由编排器决定「开始阶段无题→报错」还是「面试中途题库耗尽→提前结束」，
            // 避免因抛异常回滚整个提交事务，导致用户已作答的内容丢失、面试卡死。
            return null;
        }

        int sortOrder = session.getCurrentQuestionIndex() == null ? 0 : session.getCurrentQuestionIndex();

        InterviewQuestionRecord record = new InterviewQuestionRecord();
        record.setSessionId(sessionId);
        record.setQuestionId(question.getId());
        // 题目内容快照：防止题库后续修改影响面试记录
        record.setQuestionContent(question.getContent());
        // 规则版用题型作为“考察能力”兜底
        record.setCompetency(question.getQuestionType());
        record.setDifficulty(InterviewDifficulty.fromQuestionDifficulty(question.getDifficulty()));
        record.setSortOrder(sortOrder);
        record.setSourceType("QUESTION_BANK");
        interviewQuestionRecordService.create(record);

        // 推进会话游标：currentQuestionIndex + 1（与出题在同一事务中，由编排器保证）
        InterviewSession update = new InterviewSession();
        update.setId(sessionId);
        update.setCurrentQuestionIndex(sortOrder + 1);
        interviewSessionService.update(update);

        return record;
    }
}
