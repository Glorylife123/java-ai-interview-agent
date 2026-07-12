package com.example.interviewagent.dto;

import lombok.Data;

/**
 * 错题本分页查询筛选 DTO。
 * mastered：0 未掌握 / 1 已掌握 / null 全部。
 */
@Data
public class WrongQuestionQueryDTO {

    private Integer mastered;

    /** 页码，从 1 开始。 */
    private Integer pageNum = 1;
    /** 每页大小，默认 10。 */
    private Integer pageSize = 10;
}
