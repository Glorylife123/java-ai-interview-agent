package com.example.interviewagent.interview;

import org.springframework.util.StringUtils;

/**
 * 面试难度（字符串：简单/中等/困难）与题库 question.difficulty（整数：1/2/3）之间的映射。
 * 题库难度语义参见前端：1=简单、2=中等、3=困难。
 */
public final class InterviewDifficulty {

    public static final String EASY = "简单";
    public static final String MEDIUM = "中等";
    public static final String HARD = "困难";

    private InterviewDifficulty() {
    }

    /** 校验面试难度字符串是否合法。 */
    public static boolean isValid(String difficulty) {
        return EASY.equals(difficulty) || MEDIUM.equals(difficulty) || HARD.equals(difficulty);
    }

    /**
     * 将面试难度字符串映射为题库整数难度；无法识别时返回 null（表示不按难度过滤）。
     */
    public static Integer toQuestionDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return null;
        }
        return switch (difficulty.trim()) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
            default -> null;
        };
    }

    /** 将题库整数难度映射为面试难度字符串；无法识别时返回“中等”。 */
    public static String fromQuestionDifficulty(Integer difficulty) {
        if (difficulty == null) {
            return MEDIUM;
        }
        return switch (difficulty) {
            case 1 -> EASY;
            case 3 -> HARD;
            default -> MEDIUM;
        };
    }
}
