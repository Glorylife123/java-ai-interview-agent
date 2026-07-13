package com.example.interviewagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建面试会话入参。
 */
@Data
public class InterviewCreateDTO {

    @NotBlank(message = "岗位不能为空")
    private String position;

    /** 难度：简单/中等/困难，必填。 */
    @NotBlank(message = "难度不能为空")
    private String difficulty;

    /** 总题数，默认 5（Service 层兜底）。 */
    private Integer totalQuestionCount;
}
