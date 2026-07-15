package com.example.interviewagent.service.impl;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.controller.dto.QuestionResponse;
import com.example.interviewagent.entity.Question;
import com.example.interviewagent.mapper.QuestionMapper;
import com.example.interviewagent.mapper.QuestionTagMapper;
import com.example.interviewagent.mapper.TagMapper;
import com.example.interviewagent.service.dto.QuestionTagRelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTests {

    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private QuestionTagMapper questionTagMapper;

    private QuestionServiceImpl questionService;

    @BeforeEach
    void setUp() {
        questionService = new QuestionServiceImpl(questionMapper, tagMapper, questionTagMapper);
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
