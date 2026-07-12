package com.example.interviewagent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本列表项 VO：错题记录 + 关联题目信息。
 * 纯 MyBatis XML 用 resultType 映射（依赖 map-underscore-to-camel-case）。
 */
@Data
public class WrongQuestionVO {

    /** 错题记录 ID。 */
    private Long id;
    /** 题目 ID。 */
    private Long questionId;
    /** 题目标题（关联 question.title）。 */
    private String questionTitle;
    /** 题目所属分类（取该题第一个标签的 tag.category；question 表无 category 字段）。 */
    private String questionCategory;
    /** 题目难度（1/2/3 的整数字符串展示）。 */
    private String questionDifficulty;
    /** 题目类型（八股/场景/项目/算法）。 */
    private String questionType;
    /** 错误次数。 */
    private Integer wrongCount;
    /** 最近一次答错时间。 */
    private LocalDateTime lastWrongAt;
    /** 是否已掌握（0未掌握 / 1已掌握）。 */
    private Integer mastered;
}
