package com.example.interviewagent.interview.evaluator;

import com.example.interviewagent.entity.Question;
import com.example.interviewagent.interview.model.EvaluationContext;
import com.example.interviewagent.interview.model.EvaluationResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 规则版评分实现：基于关键句匹配。
 * <p>
 * 1. 将标准答案按中文标点（。！？；\n 等）切分为若干关键句；
 * 2. 统计用户回答中“包含”了多少关键句（核心词匹配）；
 * 3. 命中率映射为百分制得分与等级；
 * 4. 输出命中/缺失关键句与改进建议。
 */
@Component
public class RuleBasedAnswerEvaluator implements AnswerEvaluator {

    /** 关键句切分标点。 */
    private static final String SENTENCE_DELIMITERS = "[。！？；;\\n\\r]+";

    @Override
    public EvaluationResult evaluate(EvaluationContext context) {
        Question question = context.getQuestion();
        String userAnswer = context.getUserAnswer();

        String standardAnswer = question == null ? null : question.getAnswer();

        // 无标准答案：无法规则判分，给出中性结果，不阻断面试流程。
        if (!StringUtils.hasText(standardAnswer)) {
            return EvaluationResult.builder()
                    .score(0)
                    .level("较差")
                    .matchedPoints(new ArrayList<>())
                    .missingPoints(new ArrayList<>())
                    .suggestion("该题暂无标准答案，无法进行规则评分，建议参考答案解析自行复盘。")
                    .build();
        }

        List<String> keyPoints = splitKeyPoints(standardAnswer);
        // 标准答案无法切分出关键句时兜底为整句。
        if (keyPoints.isEmpty()) {
            keyPoints.add(standardAnswer.trim());
        }

        String normalizedAnswer = userAnswer == null ? "" : userAnswer.trim();

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String point : keyPoints) {
            if (isHit(normalizedAnswer, point)) {
                matched.add(point);
            } else {
                missing.add(point);
            }
        }

        int total = keyPoints.size();
        int hit = matched.size();
        int score = (int) Math.round(hit * 100.0 / total);
        String level = toLevel(score);
        String suggestion = buildSuggestion(missing);

        return EvaluationResult.builder()
                .score(score)
                .level(level)
                .matchedPoints(matched)
                .missingPoints(missing)
                .suggestion(suggestion)
                .build();
    }

    /** 按中文标点切分关键句，去空白与短噪声。 */
    private List<String> splitKeyPoints(String answer) {
        List<String> points = new ArrayList<>();
        for (String segment : answer.split(SENTENCE_DELIMITERS)) {
            String trimmed = segment.replace("，", " ").trim();
            if (StringUtils.hasText(trimmed)) {
                points.add(segment.trim());
            }
        }
        return points;
    }

    /**
     * 判断用户回答是否命中某关键句。
     * 规则：关键句直接被包含，或其“核心词”（去除标点、按逗号/空格拆出的较长词）多数被包含。
     */
    private boolean isHit(String userAnswer, String keyPoint) {
        if (!StringUtils.hasText(userAnswer)) {
            return false;
        }
        String point = keyPoint.trim();
        if (point.isEmpty()) {
            return false;
        }
        // 直接包含整句
        if (userAnswer.contains(point)) {
            return true;
        }
        // 核心词匹配：拆出长度 >= 2 的词，命中比例 >= 60% 视为命中
        List<String> coreWords = Arrays.stream(point.split("[，,、\\s]+"))
                .map(String::trim)
                .filter(w -> w.length() >= 2)
                .toList();
        if (coreWords.isEmpty()) {
            return false;
        }
        long hitWords = coreWords.stream().filter(userAnswer::contains).count();
        return hitWords * 1.0 / coreWords.size() >= 0.6;
    }

    /** 命中率百分制映射等级。 */
    private String toLevel(int score) {
        if (score >= 80) {
            return "优秀";
        }
        if (score >= 60) {
            return "良好";
        }
        if (score >= 40) {
            return "一般";
        }
        return "较差";
    }

    private String buildSuggestion(List<String> missing) {
        if (missing.isEmpty()) {
            return "回答覆盖了主要得分点，表现优秀，继续保持。";
        }
        return "建议加强以下知识点：" + String.join("；", missing);
    }
}
