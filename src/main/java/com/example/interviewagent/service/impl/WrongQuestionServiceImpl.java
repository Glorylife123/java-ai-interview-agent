package com.example.interviewagent.service.impl;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.controller.dto.QuestionResponse;
import com.example.interviewagent.dto.WrongQuestionQueryDTO;
import com.example.interviewagent.entity.Question;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.WrongQuestionMapper;
import com.example.interviewagent.service.QuestionService;
import com.example.interviewagent.service.WrongQuestionService;
import com.example.interviewagent.vo.WrongQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WrongQuestionServiceImpl implements WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionService questionService;

    // ==================== 自动加入错题本（供后续 AI 判错时调用） ====================

    @Override
    public void addOrIncrease(Long userId, Long questionId) {
        if (userId == null || questionId == null) {
            return;
        }
        wrongQuestionMapper.insertOrUpdate(userId, questionId);
    }

    @Override
    public void addOrUpdateWrongQuestion(Long userId, Long questionId) {
        addOrIncrease(userId, questionId);
    }

    @Override
    public void increase(Long userId, Long questionId) {
        addOrIncrease(userId, questionId);
    }

    // ==================== 错题本列表与维护 ====================

    @Override
    public PageResult<WrongQuestionVO> pageWrongQuestions(Long userId, WrongQuestionQueryDTO query) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        int safePageNum = normalizePage(query != null ? query.getPageNum() : null);
        int safePageSize = normalizePageSize(query != null ? query.getPageSize() : null);
        Integer mastered = query != null ? query.getMastered() : null;
        if (mastered != null && mastered != 0 && mastered != 1) {
            mastered = null; // 非法值退化为“全部”
        }
        int offset = (safePageNum - 1) * safePageSize;

        long total = wrongQuestionMapper.countPageWithQuestion(userId, mastered);
        List<WrongQuestionVO> records = total == 0
                ? List.of()
                : wrongQuestionMapper.selectPageWithQuestion(userId, mastered, offset, safePageSize);
        return new PageResult<>(total, safePageNum, safePageSize, records);
    }

    @Override
    public void removeWrongQuestion(Long userId, Long questionId) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        if (questionId == null) {
            throw new BusinessException(400, "题目ID不能为空");
        }
        if (wrongQuestionMapper.selectByUserIdAndQuestionId(userId, questionId) == null) {
            throw new BusinessException(404, "错题记录不存在");
        }
        wrongQuestionMapper.deleteByUserIdAndQuestionId(userId, questionId);
    }

    @Override
    public void markMastered(Long userId, Long questionId, Boolean mastered) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        if (questionId == null) {
            throw new BusinessException(400, "题目ID不能为空");
        }
        if (wrongQuestionMapper.selectByUserIdAndQuestionId(userId, questionId) == null) {
            throw new BusinessException(404, "错题记录不存在");
        }
        // Boolean -> Integer：null 视作「设为已掌握」(1)，沿用旧 markMastered 语义。
        int target = (mastered == null || mastered) ? 1 : 0;
        wrongQuestionMapper.updateMastered(userId, questionId, target);
    }

    // ==================== 错题再练 ====================

    @Override
    public Question getRandomWrongQuestion(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        Long questionId = wrongQuestionMapper.selectRandomWrongQuestionId(userId);
        if (questionId == null) {
            return null; // 暂无错题
        }
        // getById 不存在时抛 404，兜底“错题已删但记录残留”的场景。
        return questionService.getById(questionId);
    }

    @Override
    public QuestionResponse getRandomWrongQuestionDetail(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        Long questionId = wrongQuestionMapper.selectRandomWrongQuestionId(userId);
        if (questionId == null) {
            return null; // 无错题返回 null
        }
        // viewDetail 返回带标签的完整 QuestionResponse，可直接给前端展示题目详情。
        return questionService.viewDetail(questionId);
    }

    @Override
    public int countWrongQuestions(Long userId) {
        if (userId == null) {
            return 0;
        }
        return wrongQuestionMapper.countByUserId(userId);
    }

    // ---------- 分页参数规范化 ----------

    private int normalizePage(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
