package com.example.interviewagent.service.impl;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.controller.dto.QuestionResponse;
import com.example.interviewagent.entity.Question;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.QuestionMapper;
import com.example.interviewagent.mapper.QuestionTagMapper;
import com.example.interviewagent.mapper.TagMapper;
import com.example.interviewagent.redis.QuestionCacheLookup;
import com.example.interviewagent.redis.QuestionRedisService;
import com.example.interviewagent.service.dto.QuestionCounters;
import com.example.interviewagent.service.dto.QuestionTagRelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTests {

    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private QuestionTagMapper questionTagMapper;
    @Mock
    private QuestionRedisService questionRedisService;

    private QuestionServiceImpl questionService;

    @BeforeEach
    void setUp() {
        questionService = new QuestionServiceImpl(
                questionMapper, tagMapper, questionTagMapper, questionRedisService);
    }

    @Test
    void viewDetailUsesCachedContentAndRefreshesDynamicCounters() {
        QuestionResponse cached = new QuestionResponse();
        cached.setId(1L);
        cached.setTitle("Redis 缓存");
        cached.setViewCount(10);
        QuestionCounters counters = new QuestionCounters();
        counters.setViewCount(11);
        counters.setSubmitCount(3);
        counters.setCorrectCount(2);

        when(questionRedisService.findDetail(1L)).thenReturn(QuestionCacheLookup.valueHit(cached));
        when(questionMapper.increaseViewCount(1L)).thenReturn(1);
        when(questionMapper.selectCountersById(1L)).thenReturn(counters);

        QuestionResponse result = questionService.viewDetail(1L);

        assertEquals(11, result.getViewCount());
        assertEquals(3, result.getSubmitCount());
        verify(questionMapper, never()).selectById(1L);
        verify(questionTagMapper, never()).selectTagsByQuestionId(1L);
        verify(questionRedisService).incrementViewRank(1L);
    }

    @Test
    void viewDetailStopsAtNegativeCache() {
        when(questionRedisService.findDetail(404L)).thenReturn(QuestionCacheLookup.negativeHit());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> questionService.viewDetail(404L));

        assertEquals(404, exception.getCode());
        verify(questionMapper, never()).selectById(404L);
        verify(questionMapper, never()).increaseViewCount(404L);
    }

    @Test
    void viewDetailCachesDatabaseResultOnMiss() {
        Question question = new Question();
        question.setId(2L);
        question.setTitle("缓存穿透");
        question.setContent("解释缓存穿透");
        QuestionCounters counters = new QuestionCounters();
        counters.setViewCount(1);
        counters.setSubmitCount(0);
        counters.setCorrectCount(0);

        when(questionRedisService.findDetail(2L)).thenReturn(QuestionCacheLookup.miss());
        when(questionMapper.selectById(2L)).thenReturn(question);
        when(questionTagMapper.selectTagsByQuestionId(2L)).thenReturn(List.of());
        when(questionMapper.increaseViewCount(2L)).thenReturn(1);
        when(questionMapper.selectCountersById(2L)).thenReturn(counters);

        QuestionResponse result = questionService.viewDetail(2L);

        assertEquals("缓存穿透", result.getTitle());
        verify(questionRedisService).putDetail(2L, result);
        verify(questionRedisService).incrementViewRank(2L);
    }

    @Test
    void viewDetailCachesMissingQuestionForShortTtl() {
        when(questionRedisService.findDetail(99L)).thenReturn(QuestionCacheLookup.miss());
        when(questionMapper.selectById(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> questionService.viewDetail(99L));

        assertEquals(404, exception.getCode());
        verify(questionRedisService).putNotFound(99L);
        verify(questionMapper, never()).increaseViewCount(99L);
    }

    @Test
    void deleteEvictsQuestionCacheAfterDatabaseUpdate() {
        when(questionMapper.logicDeleteById(3L)).thenReturn(1);

        questionService.delete(3L);

        verify(questionRedisService).evictDetail(3L);
        verify(questionRedisService).removeFromViewRank(3L);
    }

    @Test
    void pageUsesCombinedFiltersAndBatchLoadsTags() {
        Question question = new Question();
        question.setId(1L);
        question.setTitle("JVM 内存模型");
        question.setContent("请说明 JVM 内存模型");
        question.setQuestionType("八股题");
        question.setDifficulty(2);

        QuestionTagRelation relation = new QuestionTagRelation();
        relation.setQuestionId(1L);
        relation.setTagId(9L);
        relation.setTagName("JVM");
        relation.setTagCategory("Java 基础");

        when(questionMapper.selectPage(eq("JVM"), eq(2), anyList(), eq(9L), eq(0), eq(20)))
                .thenReturn(List.of(question));
        when(questionMapper.countPage(eq("JVM"), eq(2), anyList(), eq(9L))).thenReturn(1L);
        when(questionTagMapper.selectRelationsByQuestionIds(List.of(1L))).thenReturn(List.of(relation));

        PageResult<QuestionResponse> result = questionService.page("JVM", 2, "八股题", 9L, 1, 20);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("JVM", result.getRecords().get(0).getTags().get(0).getName());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> typeCaptor = ArgumentCaptor.forClass(List.class);
        verify(questionMapper).selectPage(eq("JVM"), eq(2), typeCaptor.capture(), eq(9L), eq(0), eq(20));
        assertTrue(typeCaptor.getValue().contains("八股题"));
        verify(questionTagMapper).selectRelationsByQuestionIds(List.of(1L));
    }
}
