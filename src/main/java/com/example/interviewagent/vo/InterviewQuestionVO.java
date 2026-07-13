package com.example.interviewagent.vo;

import lombok.Data;

/**
 * 当前题目详情（返回给前端作答）。
 * 只返回题干快照，不含标准答案。
 */
@Data
public class InterviewQuestionVO {

    private Long questionRecordId;
    private Long questionId;
    /** 题目内容快照。 */
    private String questionContent;
    /** 题目顺序（从 0 开始）。 */
    private Integer sortOrder;
    /** 本次面试总题数。 */
    private Integer totalCount;
}
