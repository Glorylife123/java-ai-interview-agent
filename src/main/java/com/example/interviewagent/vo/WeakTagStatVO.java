package com.example.interviewagent.vo;

import lombok.Data;

/**
 * 薄弱标签统计 VO（按标签维度的错误聚合）。
 */
@Data
public class WeakTagStatVO {

    /** 标签 ID。 */
    private Long tagId;
    /** 标签名称。 */
    private String tagName;
    /** 该标签下的错误次数（is_correct = 0）。 */
    private Long wrongCount;
    /** 该标签下的总答题数（is_correct 非 NULL）。 */
    private Long totalCount;
}
