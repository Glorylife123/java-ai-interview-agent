package com.example.interviewagent.domain;

import com.example.interviewagent.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 面试题的唯一题型集合。
 * <p>
 * 数据库与 API 直接保存、返回中文值，避免向前端暴露英文枚举或选择题语义。
 */
public final class QuestionType {

    public static final String THEORY = "八股题";
    public static final String SCENARIO = "场景题";
    public static final String PROJECT = "项目题";
    public static final String ALGORITHM = "算法题";

    public static final String DEFAULT = THEORY;
    private static final Set<String> SUPPORTED_TYPES = Set.of(THEORY, SCENARIO, PROJECT, ALGORITHM);
    /**
     * 仅包含已人工确认语义的历史别名。选择题和未知值绝不在此处归类，
     * 防止读取或筛选时把旧数据错误伪装为某一种面试题。
     */
    private static final Map<String, List<String>> PERSISTENCE_VALUES = Map.of(
            THEORY, List.of(THEORY, "SHORT_ANSWER", "简答题", "八股"),
            SCENARIO, List.of(SCENARIO, "SCENARIO", "场景"),
            PROJECT, List.of(PROJECT, "PROJECT", "项目"),
            ALGORITHM, List.of(ALGORITHM, "ALGORITHM", "算法")
    );
    private static final String INVALID_TYPE_MESSAGE = "题目类型仅支持：八股题、场景题、项目题、算法题";

    private QuestionType() {
    }

    /** 创建时规范类型：未填写则使用八股题，填写了就必须属于允许集合。 */
    public static String normalizeForCreate(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT;
        }
        return requireSupported(value);
    }

    /** 更新时规范显式提交的类型：空白也是无效输入，null 由调用方解释为“不修改”。 */
    public static String requireSupported(String value) {
        String normalized = value == null ? null : value.trim();
        if (!SUPPORTED_TYPES.contains(normalized)) {
            throw new BusinessException(400, INVALID_TYPE_MESSAGE);
        }
        return normalized;
    }

    /**
     * 返回规范类型对应的持久化值集合，用于兼容已确认的历史数据筛选。
     * 调用方须先通过 {@link #requireSupported(String)} 校验公开请求参数。
     */
    public static List<String> persistenceValuesFor(String canonicalType) {
        return PERSISTENCE_VALUES.get(requireSupported(canonicalType));
    }

    /**
     * 将已确认的历史别名映射为规范中文类型。
     * 选择题、空白与未知类型返回 null，要求管理员人工分类，绝不默认归为八股题。
     */
    public static String canonicalizeKnownAlias(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return PERSISTENCE_VALUES.entrySet().stream()
                .filter(entry -> entry.getValue().contains(normalized))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
