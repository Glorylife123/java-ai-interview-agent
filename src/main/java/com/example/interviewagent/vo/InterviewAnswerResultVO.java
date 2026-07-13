package com.example.interviewagent.vo;

import lombok.Data;

import java.util.List;

/**
 * 提交回答后的即时反馈。
 * 若面试未结束，nextQuestion 为下一题；否则为 null 且 isFinished=true。
 */
@Data
public class InterviewAnswerResultVO {

    private Long questionRecordId;
    /** 本次得分。 */
    private Integer score;
    private String level;
    private List<String> matchedPoints;
    private List<String> missingPoints;
    private String suggestion;
    /** 是否面试结束。 */
    private Boolean isFinished;
    /** 未结束时返回下一题，否则为 null。 */
    private InterviewQuestionVO nextQuestion;
}
