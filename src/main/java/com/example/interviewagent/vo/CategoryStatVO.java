package com.example.interviewagent.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类统计 VO（按 tag.category 维度聚合）。
 * 仅统计 is_correct 非 NULL 的记录。
 */
@Data
public class CategoryStatVO {

    /** 分类名称（对应 tag.category）。 */
    private String category;
    /** 该分类下的答题总数（已评估）。 */
    private Long answeredCount;
    /** 该分类下的正确数。 */
    private Long correctCount;
    /** 该分类下的错误数。 */
    private Long wrongCount;
    /** 该分类下的正确率（百分比，保留一位小数）。 */
    private BigDecimal correctRate;
}
