package com.example.interviewagent.vo;

import lombok.Data;

import java.util.List;

/**
 * 面试单题答题明细视图对象。
 * 由编排器将 题目记录 / 答案 / 评分 三表在 sessionId 维度按 sortOrder 对齐组装而成。
 * <p>
 * 设计说明：
 * - 题目内容取 interview_question_record.question_content 快照，并非题库当前内容，
 *   因此即便题库后续被修改，回看依旧反映「面试当时的题目」。
 * - matchedPoints / missingPoints 在持久化层以 \"\\n\" 连接，此处拆成 List 便于前端直接渲染标签。
 * - 当用户中途结束面试（题库耗尽导致提前 FINISHED）时，本次面试可能不达到 totalQuestionCount；
 *   因此本明细只包含「已出题 + 已作答 + 已评分」的题目，未作答的不出现在列表中。
 */
@Data
public class InterviewAnswerDetailVO {

    /** 面试题目记录 ID（对应 interview_question_record.id）。 */
    private Long questionRecordId;
    /** 题库题目 ID（便于后续做「跳转题库该题」扩展）。 */
    private Long questionId;
    /** 题目顺序（从 0 开始）。 */
    private Integer sortOrder;
    /** 题目内容快照。 */
    private String questionContent;
    /** 考察能力（本规则版用题型/分类兜底）。 */
    private String competency;
    /** 题目难度：简单/中等/困难。 */
    private String difficulty;
    /** 题型（冗余自 question，如 \"问答题\"），前端用于展示标签。 */
    private String questionType;

    // ---- 用户作答 ----
    /** 用户作答内容；未作答时为 null。 */
    private String answerContent;
    /** 答题耗时（秒）；未作答时为 null。 */
    private Integer durationSeconds;
    /** 答题是否已提交（用于 UI 决定是否展示「已作答 / 未作答」状态）。 */
    private Boolean answered;

    // ---- 评分反馈 ----
    /** 本次得分（0-100）；未评分时为 null。 */
    private Integer score;
    /** 满分，恒为 100。 */
    private Integer maxScore;
    /** 等级：优秀/良好/一般/较差。 */
    private String level;
    /** 命中得分点列表。 */
    private List<String> matchedPoints;
    /** 缺失得分点列表。 */
    private List<String> missingPoints;
    /** 改进建议。 */
    private String suggestion;
}
