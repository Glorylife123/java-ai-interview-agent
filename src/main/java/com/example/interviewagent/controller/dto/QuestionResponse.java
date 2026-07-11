package com.example.interviewagent.controller.dto;

import com.example.interviewagent.domain.QuestionType;
import com.example.interviewagent.entity.Question;
import com.example.interviewagent.entity.Tag;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 题目对外响应：题目基础信息加上已关联标签。 */
@Data
public class QuestionResponse {

    private Long id;
    private String title;
    private String content;
    private String questionType;
    private Integer difficulty;
    private String answer;
    private String answerAnalysis;
    private String source;
    private Integer viewCount;
    private Integer submitCount;
    private Integer correctCount;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Tag> tags = new ArrayList<>();

    public static QuestionResponse from(Question question, List<Tag> tags) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setTitle(question.getTitle());
        response.setContent(question.getContent());
        // 仅把已确认语义的历史别名转为中文类型；选择题和未知旧值返回 null，要求人工分类。
        response.setQuestionType(QuestionType.canonicalizeKnownAlias(question.getQuestionType()));
        response.setDifficulty(question.getDifficulty());
        response.setAnswer(question.getAnswer());
        response.setAnswerAnalysis(question.getAnswerAnalysis());
        response.setSource(question.getSource());
        response.setViewCount(question.getViewCount());
        response.setSubmitCount(question.getSubmitCount());
        response.setCorrectCount(question.getCorrectCount());
        response.setStatus(question.getStatus());
        response.setCreatedBy(question.getCreatedBy());
        response.setCreatedAt(question.getCreatedAt());
        response.setUpdatedAt(question.getUpdatedAt());
        response.setTags(tags == null ? new ArrayList<>() : tags);
        return response;
    }
}
