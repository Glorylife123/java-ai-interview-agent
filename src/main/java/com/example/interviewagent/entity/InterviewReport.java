package com.example.interviewagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试报告实体，对应数据库表 interview_report。
 * dimension_scores_json / suggestions_json 为 JSON 字符串，由 Service/策略层用 Jackson 序列化写入。
 * session_id 唯一（uk_session_id），更新报告采用「先删后插」。
 */
@Data
public class InterviewReport {

    private Long id;
    private Long sessionId;
    /** 总体得分（0-100）。 */
    private Integer overallScore;
    private String summary;
    /** 各维度得分（JSON 字符串）。 */
    private String dimensionScoresJson;
    /** 学习建议（JSON 数组字符串）。 */
    private String suggestionsJson;
    /** 生成者：RULE / AI。 */
    private String generatorType;
    private LocalDateTime createdAt;
}
