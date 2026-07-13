package com.example.interviewagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交面试回答入参。
 */
@Data
public class InterviewAnswerSubmitDTO {

    @NotNull(message = "题目记录ID不能为空")
    private Long questionRecordId;

    @NotBlank(message = "回答内容不能为空")
    private String answerContent;

    /** 答题耗时（秒），非必填。 */
    private Integer durationSeconds;
}
