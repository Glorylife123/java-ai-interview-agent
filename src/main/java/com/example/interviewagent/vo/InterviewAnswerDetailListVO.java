package com.example.interviewagent.vo;

import lombok.Data;

import java.util.List;

/**
 * 面试整场答题明细视图对象：外层包装，含会话元信息与逐题明细列表。
 * 供「查看我的答题情况」接口返回。
 */
@Data
public class InterviewAnswerDetailListVO {

    /** 会话 ID。 */
    private Long sessionId;
    /** 面试标题。 */
    private String title;
    /** 岗位。 */
    private String position;
    /** 难度：简单/中等/困难。 */
    private String difficulty;
    /** 会话状态：CREATED/IN_PROGRESS/FINISHED/CANCELLED。 */
    private String status;
    /** 预设总题数。 */
    private Integer totalQuestionCount;
    /** 已作答题数（等于明细列表大小）。 */
    private Integer answeredCount;
    /** 逐题答题明细，按 sortOrder 升序。 */
    private List<InterviewAnswerDetailVO> details;
}
