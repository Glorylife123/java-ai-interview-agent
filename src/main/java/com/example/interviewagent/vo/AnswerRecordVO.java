package com.example.interviewagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 历史答题记录分页项。
 * isCorrect / score 目前恒为 null（待 AI 评估），前端据此展示“待AI评估”。
 */
@Data
public class AnswerRecordVO {

    private Long id;
    private Long questionId;
    private String questionTitle;
    private String userAnswer;
    /** 是否正确：0 错误 / 1 正确 / null 待评估。 */
    private Integer isCorrect;
    /** 得分，目前为 null。 */
    private Integer score;
    private Integer timeCostSeconds;
    private LocalDateTime createdAt;
}
