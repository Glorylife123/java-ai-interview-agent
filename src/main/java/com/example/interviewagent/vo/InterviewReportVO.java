package com.example.interviewagent.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 面试报告视图对象。
 * dimensionScores / suggestions 已由 Service 层从报告实体的 JSON 字段反序列化为结构化对象。
 */
@Data
public class InterviewReportVO {

    private Long sessionId;
    private Integer overallScore;
    private String summary;
    /** 各维度得分。 */
    private Map<String, Integer> dimensionScores;
    /** 学习建议。 */
    private List<String> suggestions;
    private String generatorType;
    private LocalDateTime createdAt;
}
