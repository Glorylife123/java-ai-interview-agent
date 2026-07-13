package com.example.interviewagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试评分实体，对应数据库表 interview_evaluation。
 * matched_points / missing_points 以逗号分隔的关键句形式持久化（规则版）。
 */
@Data
public class InterviewEvaluation {

    private Long id;
    private Long sessionId;
    private Long questionRecordId;
    private Long answerId;
    /** 得分（0-100）。 */
    private Integer score;
    private Integer maxScore;
    /** 等级：优秀/良好/一般/较差。 */
    private String level;
    /** 命中得分点（逗号分隔）。 */
    private String matchedPoints;
    /** 缺失得分点（逗号分隔）。 */
    private String missingPoints;
    private String suggestion;
    /** 评分者：RULE / AI / MANUAL。 */
    private String evaluatorType;
    private LocalDateTime createdAt;
}
