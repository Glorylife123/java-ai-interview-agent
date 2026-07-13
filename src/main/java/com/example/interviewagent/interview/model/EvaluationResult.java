package com.example.interviewagent.interview.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评分结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

    private Integer score;
    /** 等级：优秀/良好/一般/较差。 */
    private String level;
    private List<String> matchedPoints;
    private List<String> missingPoints;
    private String suggestion;
}
