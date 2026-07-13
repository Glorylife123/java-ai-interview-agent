package com.example.interviewagent.interview.report;

import com.example.interviewagent.entity.InterviewEvaluation;
import com.example.interviewagent.entity.InterviewQuestionRecord;
import com.example.interviewagent.entity.InterviewReport;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.service.InterviewEvaluationService;
import com.example.interviewagent.service.InterviewQuestionRecordService;
import com.example.interviewagent.service.InterviewReportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则版报告生成实现：
 * 1. 汇总各题得分求总体均分；
 * 2. 按“考察能力/题型”维度求各维度均分；
 * 3. 依据总分区间生成总结评语；
 * 4. 从各题缺失得分点提取高频知识点，组合为学习建议；
 * 5. 维度得分与建议序列化为 JSON 存入报告表（session_id 唯一，先删后插）。
 */
@Component
@RequiredArgsConstructor
public class RuleBasedInterviewReportGenerator implements InterviewReportGenerator {

    private final InterviewQuestionRecordService interviewQuestionRecordService;
    private final InterviewEvaluationService interviewEvaluationService;
    private final InterviewReportService interviewReportService;
    private final ObjectMapper objectMapper;

    @Override
    public InterviewReport generateReport(Long sessionId) {
        List<InterviewQuestionRecord> records = interviewQuestionRecordService.listBySessionId(sessionId);
        List<InterviewEvaluation> evaluations = interviewEvaluationService.listBySessionId(sessionId);

        // questionRecordId -> 维度（考察能力/题型），用于维度汇总
        Map<Long, String> recordDimension = new LinkedHashMap<>();
        for (InterviewQuestionRecord r : records) {
            recordDimension.put(r.getId(),
                    StringUtils.hasText(r.getCompetency()) ? r.getCompetency() : "综合");
        }

        int overallScore = computeOverallScore(evaluations);
        Map<String, Integer> dimensionScores = computeDimensionScores(evaluations, recordDimension);
        List<String> suggestions = buildSuggestions(evaluations);
        String summary = buildSummary(overallScore);

        InterviewReport report = new InterviewReport();
        report.setSessionId(sessionId);
        report.setOverallScore(overallScore);
        report.setSummary(summary);
        report.setDimensionScoresJson(toJson(dimensionScores));
        report.setSuggestionsJson(toJson(suggestions));
        report.setGeneratorType("RULE");

        // session_id 唯一：先删后插，支持重复生成
        interviewReportService.deleteBySessionId(sessionId);
        interviewReportService.create(report);
        return report;
    }

    /** 总体得分 = 各题得分均值（忽略 null 得分）。 */
    private int computeOverallScore(List<InterviewEvaluation> evaluations) {
        List<Integer> scores = evaluations.stream()
                .map(InterviewEvaluation::getScore)
                .filter(s -> s != null)
                .toList();
        if (scores.isEmpty()) {
            return 0;
        }
        double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        return (int) Math.round(avg);
    }

    /** 各维度得分 = 该维度下各题得分均值。 */
    private Map<String, Integer> computeDimensionScores(List<InterviewEvaluation> evaluations,
                                                        Map<Long, String> recordDimension) {
        Map<String, List<Integer>> grouped = new LinkedHashMap<>();
        for (InterviewEvaluation e : evaluations) {
            if (e.getScore() == null) {
                continue;
            }
            String dimension = recordDimension.getOrDefault(e.getQuestionRecordId(), "综合");
            grouped.computeIfAbsent(dimension, k -> new ArrayList<>()).add(e.getScore());
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        grouped.forEach((dimension, scores) -> {
            double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            result.put(dimension, (int) Math.round(avg));
        });
        return result;
    }

    /**
     * 学习建议：统计所有缺失得分点出现频次，取前若干条组合为建议。
     */
    private List<String> buildSuggestions(List<InterviewEvaluation> evaluations) {
        Map<String, Integer> missingFreq = new LinkedHashMap<>();
        for (InterviewEvaluation e : evaluations) {
            if (!StringUtils.hasText(e.getMissingPoints())) {
                continue;
            }
            for (String point : e.getMissingPoints().split("\n")) {
                String p = point.trim();
                if (StringUtils.hasText(p)) {
                    missingFreq.merge(p, 1, Integer::sum);
                }
            }
        }
        List<String> suggestions = missingFreq.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(5)
                .map(entry -> "加强「" + entry.getKey() + "」相关知识点的理解与表达")
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (suggestions.isEmpty()) {
            suggestions.add("整体表现良好，建议持续巩固核心知识点，保持稳定输出。");
        }
        return suggestions;
    }

    private String buildSummary(int overallScore) {
        if (overallScore >= 80) {
            return "总体表现优秀，知识点覆盖全面，建议冲击更高层次岗位。";
        }
        if (overallScore >= 60) {
            return "总体表现良好，基础扎实，重点加强薄弱维度即可更进一步。";
        }
        return "总体表现有待提升，建议系统复习核心知识点后再次练习。";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "报告数据序列化失败");
        }
    }
}
