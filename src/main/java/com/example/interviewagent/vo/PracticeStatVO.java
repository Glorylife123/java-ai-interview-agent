package com.example.interviewagent.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 练习总览统计 VO。
 * 仅统计 is_correct 非 NULL（已 AI 评估）的记录；avgScore 全 NULL 时返回 0。
 */
@Data
public class PracticeStatVO {

    /** 总答题数（已评估）。 */
    private Long totalAnswered;
    /** 正确数。 */
    private Long correctCount;
    /** 错误数。 */
    private Long wrongCount;
    /** 正确率（百分比，保留一位小数，如 66.7）。分母为 0 时返回 0.0。 */
    private BigDecimal correctRate;
    /** 错题本题目数量（来自 wrong_question 表）。 */
    private Long wrongBookCount;
    /** 平均得分，score 均为 NULL 时返回 0。 */
    private BigDecimal avgScore;
}
