package com.example.interviewagent.service.impl;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.controller.dto.QuestionResponse;
import com.example.interviewagent.controller.dto.QuestionUpsertRequest;
import com.example.interviewagent.controller.dto.HotQuestionResponse;
import com.example.interviewagent.domain.QuestionType;
import com.example.interviewagent.entity.Question;
import com.example.interviewagent.entity.Tag;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.QuestionMapper;
import com.example.interviewagent.mapper.QuestionTagMapper;
import com.example.interviewagent.mapper.TagMapper;
import com.example.interviewagent.redis.QuestionCacheLookup;
import com.example.interviewagent.redis.QuestionRankEntry;
import com.example.interviewagent.redis.QuestionRedisService;
import com.example.interviewagent.service.QuestionService;
import com.example.interviewagent.service.dto.QuestionCounters;
import com.example.interviewagent.service.dto.QuestionTagRelation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final int HOT_CACHE_SIZE = 50;

    private final QuestionMapper questionMapper;
    private final TagMapper tagMapper;
    private final QuestionTagMapper questionTagMapper;
    private final QuestionRedisService questionRedisService;

    @Override
    @Transactional
    public QuestionResponse create(QuestionUpsertRequest request) {
        validateCreateRequest(request);
        Question question = toQuestion(request);
        // 创建时未填写类型默认八股题；填写了就必须是四种中文类型之一。
        question.setQuestionType(QuestionType.normalizeForCreate(request.getQuestionType()));
        fillDefaultValues(question);
        questionMapper.insert(question);
        // 新建请求中 null 与空数组都代表“先不关联标签”。
        if (request.getTagIds() != null) {
            replaceTags(question.getId(), request.getTagIds());
        }
        QuestionResponse response = toResponse(questionMapper.selectById(question.getId()));
        questionRedisService.evictDetail(question.getId());
        return response;
    }

    @Override
    public Question getById(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(404, "题目不存在");
        }
        return question;
    }

    @Override
    public QuestionResponse viewDetail(Long id) {
        QuestionCacheLookup lookup = questionRedisService.findDetail(id);
        if (lookup.hit() && lookup.value() == null) {
            throw new BusinessException(404, "题目不存在");
        }

        QuestionResponse response = lookup.hit() ? lookup.value() : loadDetailForCache(id);
        if (questionMapper.increaseViewCount(id) == 0) {
            questionRedisService.evictDetail(id);
            throw new BusinessException(404, "题目不存在");
        }

        QuestionCounters counters = questionMapper.selectCountersById(id);
        if (counters == null) {
            questionRedisService.evictDetail(id);
            throw new BusinessException(404, "题目不存在");
        }
        applyCounters(response, counters);
        if (!lookup.hit()) {
            questionRedisService.putDetail(id, response);
        }
        questionRedisService.incrementViewRank(id);
        return response;
    }

    @Override
    public List<HotQuestionResponse> listHot(Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 10 : Math.min(limit, HOT_CACHE_SIZE);
        return questionRedisService.findHotList(safeLimit)
                .orElseGet(() -> loadHotListFromRank(safeLimit));
    }

    private List<HotQuestionResponse> loadHotListFromRank(int safeLimit) {
        List<QuestionRankEntry> ranks = questionRedisService.topViewed(HOT_CACHE_SIZE);
        if (ranks.isEmpty()) {
            return List.of();
        }
        List<HotQuestionResponse> result = new ArrayList<>(ranks.size());
        for (QuestionRankEntry rank : ranks) {
            QuestionCacheLookup lookup = questionRedisService.findDetail(rank.questionId());
            if (lookup.hit() && lookup.value() == null) {
                continue;
            }
            try {
                QuestionResponse detail = lookup.hit() ? lookup.value() : loadDetailForCache(rank.questionId());
                if (!lookup.hit()) {
                    questionRedisService.putDetail(rank.questionId(), detail);
                }
                result.add(new HotQuestionResponse(rank.questionId(), detail.getTitle(), rank.score()));
            } catch (BusinessException e) {
                if (e.getCode() != 404) {
                    throw e;
                }
            }
        }
        questionRedisService.putHotList(result);
        return result.subList(0, Math.min(safeLimit, result.size()));
    }

    @Override
    public PageResult<QuestionResponse> page(String keyword, Integer difficulty, String questionType,
                                             Long tagId, Integer pageNum, Integer pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePageNum - 1) * safePageSize;
        // 未传类型即不筛选；显式传入规范中文类型后，展开为已确认的历史别名集合。
        // 例如“八股题”同时匹配 SHORT_ANSWER、简答题和八股，不会包含选择题或未知旧值。
        List<String> questionTypeValues = questionType == null
                ? null
                : QuestionType.persistenceValuesFor(questionType);
        List<Question> records = questionMapper.selectPage(keyword, difficulty, questionTypeValues, tagId, offset, safePageSize);
        long total = questionMapper.countPage(keyword, difficulty, questionTypeValues, tagId);

        if (records.isEmpty()) {
            return new PageResult<>(total, safePageNum, safePageSize, Collections.emptyList());
        }

        List<Long> questionIds = records.stream().map(Question::getId).toList();
        Map<Long, List<Tag>> tagsByQuestionId = questionTagMapper.selectRelationsByQuestionIds(questionIds)
                .stream()
                .collect(Collectors.groupingBy(
                        QuestionTagRelation::getQuestionId,
                        Collectors.mapping(QuestionTagRelation::toTag, Collectors.toList())
                ));
        List<QuestionResponse> responses = records.stream()
                .map(question -> QuestionResponse.from(question,
                        tagsByQuestionId.getOrDefault(question.getId(), Collections.emptyList())))
                .toList();
        return new PageResult<>(total, safePageNum, safePageSize, responses);
    }

    @Override
    @Transactional
    public QuestionResponse update(Long id, QuestionUpsertRequest request) {
        if (request == null) {
            throw new BusinessException(400, "请求参数不能为空");
        }
        getById(id);
        Question question = toQuestion(request);
        question.setId(id);
        // null 保持部分更新语义；显式提交（包括空白）的值必须为四种中文类型之一。
        if (request.getQuestionType() != null) {
            question.setQuestionType(QuestionType.requireSupported(request.getQuestionType()));
        }
        // 允许纯标签编辑：若无题目字段需要更新，跳过空 SET 的 SQL 更新。
        if (hasQuestionFieldsToUpdate(question)) {
            questionMapper.updateById(question);
        }
        // null 保持现有关联（兼容旧调用方）；[] 显式清空；非空列表完整替换。
        if (request.getTagIds() != null) {
            replaceTags(id, request.getTagIds());
        }
        QuestionResponse response = toResponse(getById(id));
        questionRedisService.evictDetail(id);
        questionRedisService.evictHotList();
        return response;
    }

    @Override
    public void delete(Long id) {
        if (questionMapper.logicDeleteById(id) == 0) {
            throw new BusinessException(404, "题目不存在");
        }
        questionRedisService.evictDetail(id);
        questionRedisService.removeFromViewRank(id);
    }

    @Override
    public void bindTag(Long questionId, Long tagId) {
        getById(questionId);
        if (tagMapper.selectById(tagId) == null) {
            throw new BusinessException(404, "标签不存在");
        }
        questionTagMapper.insertIgnore(questionId, tagId);
        questionRedisService.evictDetail(questionId);
    }

    @Override
    public List<Tag> listTags(Long questionId) {
        getById(questionId);
        return questionTagMapper.selectTagsByQuestionId(questionId);
    }

    @Override
    public void increaseSubmitCount(Long questionId, Integer isCorrect) {
        questionMapper.increaseSubmitCount(questionId, isCorrect);
        questionRedisService.evictDetail(questionId);
    }

    private QuestionResponse loadDetailForCache(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            questionRedisService.putNotFound(id);
            throw new BusinessException(404, "题目不存在");
        }
        return toResponse(question);
    }

    private void applyCounters(QuestionResponse response, QuestionCounters counters) {
        response.setViewCount(counters.getViewCount());
        response.setSubmitCount(counters.getSubmitCount());
        response.setCorrectCount(counters.getCorrectCount());
    }

    /** 整体替换一题的标签：先校验全部 tagId，再删除旧关联，避免非法标签导致旧关联被误清空。 */
    private void replaceTags(Long questionId, List<Long> requestedTagIds) {
        List<Long> tagIds = normalizeTagIds(requestedTagIds);
        for (Long tagId : tagIds) {
            if (tagMapper.selectById(tagId) == null) {
                throw new BusinessException(400, "标签不存在或已被删除：" + tagId);
            }
        }
        questionTagMapper.deleteByQuestionId(questionId);
        if (!tagIds.isEmpty()) {
            questionTagMapper.insertBatch(questionId, tagIds);
        }
    }

    private List<Long> normalizeTagIds(List<Long> requestedTagIds) {
        if (requestedTagIds == null || requestedTagIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long tagId : requestedTagIds) {
            if (tagId == null || tagId <= 0) {
                throw new BusinessException(400, "标签 ID 必须为正整数");
            }
            uniqueIds.add(tagId);
        }
        return new ArrayList<>(uniqueIds);
    }

    private QuestionResponse toResponse(Question question) {
        return QuestionResponse.from(question, questionTagMapper.selectTagsByQuestionId(question.getId()));
    }

    private Question toQuestion(QuestionUpsertRequest request) {
        Question question = new Question();
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setQuestionType(request.getQuestionType());
        question.setDifficulty(request.getDifficulty());
        question.setAnswer(request.getAnswer());
        question.setAnswerAnalysis(request.getAnswerAnalysis());
        question.setSource(request.getSource());
        question.setStatus(request.getStatus());
        question.setCreatedBy(request.getCreatedBy());
        return question;
    }

    private void validateCreateRequest(QuestionUpsertRequest request) {
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(400, "题目标题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new BusinessException(400, "题目内容不能为空");
        }
    }

    private boolean hasQuestionFieldsToUpdate(Question question) {
        return question.getTitle() != null || question.getContent() != null || question.getQuestionType() != null
                || question.getDifficulty() != null || question.getAnswer() != null || question.getAnswerAnalysis() != null
                || question.getSource() != null || question.getStatus() != null;
    }

    private void fillDefaultValues(Question question) {
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
