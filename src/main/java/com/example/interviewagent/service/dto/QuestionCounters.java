package com.example.interviewagent.service.dto;

import lombok.Data;

/** 题目高频变化计数，单独查询以避免缓存详情返回过期计数。 */
@Data
public class QuestionCounters {

    private Integer viewCount;
    private Integer submitCount;
    private Integer correctCount;
}
