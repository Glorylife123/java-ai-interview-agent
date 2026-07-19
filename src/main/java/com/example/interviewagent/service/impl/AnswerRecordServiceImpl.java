package com.example.interviewagent.service.impl;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.dto.AnswerSubmitDTO;
import com.example.interviewagent.entity.AnswerRecord;
import com.example.interviewagent.entity.Question;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.AnswerRecordMapper;
import com.example.interviewagent.redis.AnswerSubmitLockService;
import com.example.interviewagent.redis.PracticeStatRedisService;
import com.example.interviewagent.service.AnswerRecordService;
import com.example.interviewagent.service.QuestionService;
import com.example.interviewagent.vo.AnswerResultVO;
import com.example.interviewagent.vo.AnswerRecordVO;
import com.example.interviewagent.vo.CategoryStatVO;
import com.example.interviewagent.vo.DailyStatVO;
import com.example.interviewagent.vo.PracticeStatVO;
import com.example.interviewagent.vo.WeakTagStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerRecordServiceImpl implements AnswerRecordService {

    private final AnswerRecordMapper answerRecordMapper;
    private final QuestionService questionService;
    private final PracticeStatRedisService practiceStatRedisService;
    private final AnswerSubmitLockService answerSubmitLockService;
    // 错题本接入点：保持为「仅声明、不注入」可避免本阶段无 AI 判分时的循环依赖；
    // 第 11 步接入 AI 判分后，在此注入并调用 addOrIncrease(userId, questionId) 即可自动加入错题本。
    // private final WrongQuestionService wrongQuestionService;

    @Override
    public AnswerResultVO submitAnswer(Long userId, AnswerSubmitDTO dto) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        if (dto == null || dto.getQuestionId() == null || !StringUtils.hasText(dto.getUserAnswer())) {
            throw new BusinessException(400, "题目ID和回答内容不能为空");
        }
        // 校验题目存在（不存在时 getById 抛 404）。
        Question question = questionService.getById(dto.getQuestionId());
        if (!answerSubmitLockService.tryLock(userId, dto.getQuestionId())) {
            throw new BusinessException(429, "提交过于频繁，请稍后再试");
        }

        // 本迭代（第 5 步）不接入 AI 判断：isCorrect / score 保持 null，answerSource 固定 MANUAL。
        // 严格约束：不做正误判断、不评分、不写入 WrongQuestion 错题本。
        AnswerRecord record = new AnswerRecord();
        record.setUserId(userId);
        record.setQuestionId(dto.getQuestionId());
        record.setUserAnswer(dto.getUserAnswer());
        record.setIsCorrect(null);
        record.setScore(null);
        record.setTimeCostSeconds(dto.getTimeCostSeconds());
        record.setAnswerSource("MANUAL");
        record.setCreatedAt(LocalDateTime.now());
        answerRecordMapper.insert(record);
        practiceStatRedisService.evictOverview(userId);

        // TODO(step11 接入 AI 评分后补齐以下逻辑)：
        // 1. 调用 AI 评分服务得到 isCorrect / score，回填到已 insert 的 record（或改为先评分再 insert）。
        // 2. 仅当判错（isCorrect == 0）时调用：
        //      wrongQuestionService.addOrIncrease(userId, question.getId());
        //    该方法已实现 UPSERT（ON DUPLICATE KEY UPDATE wrong_count+1），无需额外判断是否已存在。
        // 3. 调用 questionService.increaseSubmitCount(question.getId(), isCorrect) 累加题目统计。
        // 当前 is_correct 仍为 null，统计模块与错题本均无数据，属阶段性预期行为。

        AnswerResultVO vo = new AnswerResultVO();
        vo.setQuestionId(question.getId());
        vo.setQuestionTitle(question.getTitle());
        vo.setUserAnswer(dto.getUserAnswer());
        vo.setStandardAnswer(question.getAnswer());
        vo.setAnalysis(question.getAnswerAnalysis());
        vo.setSubmitTime(record.getCreatedAt());
        return vo;
    }

    @Override
    public PageResult<AnswerRecordVO> pageRecords(Long userId, Integer pageNum, Integer pageSize) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePageNum - 1) * safePageSize;

        long total = answerRecordMapper.countByUserId(userId);
        List<AnswerRecordVO> records = total == 0
                ? List.of()
                : answerRecordMapper.selectPageByUserId(userId, offset, safePageSize);
        return new PageResult<>(total, safePageNum, safePageSize, records);
    }

    @Override
    public PracticeStatVO getOverviewStat(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        return practiceStatRedisService.findOverview(userId)
                .orElseGet(() -> loadOverviewStat(userId));
    }

    private PracticeStatVO loadOverviewStat(Long userId) {
        PracticeStatVO vo = answerRecordMapper.selectOverviewStat(userId);
        if (vo == null) {
            // 无已评估记录时返回全 0 默认 VO（不抛异常）。
            vo = new PracticeStatVO();
            vo.setTotalAnswered(0L);
            vo.setCorrectCount(0L);
            vo.setWrongCount(0L);
            vo.setCorrectRate(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
            vo.setWrongBookCount(0L);
            vo.setAvgScore(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
            practiceStatRedisService.putOverview(userId, vo);
            return vo;
        }
        // COUNT/SUM 在空集上由 MyBatis 聚合返回 NULL，统一兜底为 0。
        long total = nullToZero(vo.getTotalAnswered());
        long correct = nullToZero(vo.getCorrectCount());
        long wrong = nullToZero(vo.getWrongCount());
        vo.setTotalAnswered(total);
        vo.setCorrectCount(correct);
        vo.setWrongCount(wrong);
        // wrongBookCount 错题本数量独立查询。
        vo.setWrongBookCount(answerRecordMapper.countWrongBook(userId));
        // correctRate = correct * 100 / (correct + wrong)，分母 0 返回 0.0。
        vo.setCorrectRate(ratePercent(correct, correct + wrong));
        // avgScore 全 NULL 时数据库返回 null，兜底为 0 并保留一位小数。
        vo.setAvgScore(nullToZeroScale1(vo.getAvgScore()));
        practiceStatRedisService.putOverview(userId, vo);
        return vo;
    }

    @Override
    public List<CategoryStatVO> getCategoryStats(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        List<CategoryStatVO> list = answerRecordMapper.selectCategoryStats(userId);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        list.forEach(c -> {
            long answered = nullToZero(c.getAnsweredCount());
            long correct = nullToZero(c.getCorrectCount());
            c.setAnsweredCount(answered);
            c.setCorrectCount(correct);
            c.setWrongCount(nullToZero(c.getWrongCount()));
            c.setCorrectRate(ratePercent(correct, answered));
        });
        return list;
    }

    @Override
    public List<WeakTagStatVO> getWeakTagStats(Long userId, Integer limit) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        int safeLimit = limit == null || limit < 1 ? 5 : Math.min(limit, 50);
        List<WeakTagStatVO> list = answerRecordMapper.selectWeakTagStats(userId, safeLimit);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        list.forEach(t -> {
            t.setWrongCount(nullToZero(t.getWrongCount()));
            t.setTotalCount(nullToZero(t.getTotalCount()));
        });
        return list;
    }

    @Override
    public List<DailyStatVO> getDailyStats(Long userId, Integer days) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        int safeDays = days == null || days < 1 ? 30 : Math.min(days, 365);
        List<DailyStatVO> list = answerRecordMapper.selectDailyStats(userId, safeDays);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        list.forEach(d -> d.setCount(nullToZero(d.getCount())));
        return list;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    /** Long 空安全兜底为 0。 */
    private long nullToZero(Long v) {
        return v == null ? 0L : v;
    }

    /** 百分比正确率：(part / total) * 100，保留一位小数；分母为 0 返回 0.0。 */
    private BigDecimal ratePercent(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    /** avgScore 兜底：null/0 -> "0.0"；保留一位小数。 */
    private BigDecimal nullToZeroScale1(BigDecimal v) {
        if (v == null || v.signum() == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return v.setScale(1, RoundingMode.HALF_UP);
    }
}
