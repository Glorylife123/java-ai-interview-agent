package com.example.interviewagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交答题记录入参。
 * 仅承载题目、用户答案与耗时，不包含任何正误/评分字段（本迭代不接入 AI 判断）。
 */
@Data
public class AnswerSubmitDTO {

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotBlank(message = "回答内容不能为空")
    private String userAnswer;

    /** 答题耗时（秒），非必填。 */
    private Integer timeCostSeconds;
}
