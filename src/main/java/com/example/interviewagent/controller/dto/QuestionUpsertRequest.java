package com.example.interviewagent.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 管理端题目新增/编辑请求。
 * tagIds 为完整标签集合：空数组表示清空标签；编辑时 null 表示保持既有标签不变。
 */
@Data
public class QuestionUpsertRequest {

    private String title;
    private String content;
    private String questionType;
    private Integer difficulty;
    private String answer;
    private String answerAnalysis;
    private String source;
    private Integer status;
    private Long createdBy;
    private List<Long> tagIds;
}
