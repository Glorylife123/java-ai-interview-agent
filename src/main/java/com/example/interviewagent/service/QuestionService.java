package com.example.interviewagent.service;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.entity.Question;
import com.example.interviewagent.entity.Tag;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.QuestionMapper;
import com.example.interviewagent.mapper.QuestionTagMapper;
import com.example.interviewagent.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final TagMapper tagMapper;
    private final QuestionTagMapper questionTagMapper;

    public Question create(Question question) {
        if (question == null || !StringUtils.hasText(question.getTitle())) {
            throw new BusinessException(400, "题目标题不能为空");
        }
        if (!StringUtils.hasText(question.getContent())) {
            throw new BusinessException(400, "题目内容不能为空");
        }
        fillDefaultValues(question);
        questionMapper.insert(question);
        return getById(question.getId());
    }

    public Question getById(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(404, "题目不存在");
        }
        return question;
    }

    public Question viewDetail(Long id) {
        questionMapper.increaseViewCount(id);
        return getById(id);
    }

    public PageResult<Question> page(String keyword, Integer difficulty, Long tagId, Integer pageNum, Integer pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePageNum - 1) * safePageSize;
        List<Question> records = questionMapper.selectPage(keyword, difficulty, tagId, offset, safePageSize);
        long total = questionMapper.countPage(keyword, difficulty, tagId);
        return new PageResult<>(total, safePageNum, safePageSize, records);
    }

    public Question update(Long id, Question question) {
        question.setId(id);
        int rows = questionMapper.updateById(question);
        if (rows == 0) {
            throw new BusinessException(404, "题目不存在或未修改");
        }
        return getById(id);
    }

    public void delete(Long id) {
        if (questionMapper.logicDeleteById(id) == 0) {
            throw new BusinessException(404, "题目不存在");
        }
    }

    public void bindTag(Long questionId, Long tagId) {
        getById(questionId);
        if (tagMapper.selectById(tagId) == null) {
            throw new BusinessException(404, "标签不存在");
        }
        questionTagMapper.insertIgnore(questionId, tagId);
    }

    public List<Tag> listTags(Long questionId) {
        getById(questionId);
        return questionTagMapper.selectTagsByQuestionId(questionId);
    }

    public void increaseSubmitCount(Long questionId, Integer isCorrect) {
        questionMapper.increaseSubmitCount(questionId, isCorrect);
    }

    private void fillDefaultValues(Question question) {
        if (!StringUtils.hasText(question.getQuestionType())) {
            question.setQuestionType("SHORT_ANSWER");
        }
        if (question.getDifficulty() == null) {
            question.setDifficulty(1);
        }
        if (question.getViewCount() == null) {
            question.setViewCount(0);
        }
        if (question.getSubmitCount() == null) {
            question.setSubmitCount(0);
        }
        if (question.getCorrectCount() == null) {
            question.setCorrectCount(0);
        }
        if (question.getStatus() == null) {
            question.setStatus(1);
        }
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
}
