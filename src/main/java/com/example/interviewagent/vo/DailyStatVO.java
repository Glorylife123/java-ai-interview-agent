package com.example.interviewagent.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 每日练习趋势 VO（前端可展示折线图）。
 * 仅返回有答题记录的日期，缺失日期不补 0。
 */
@Data
public class DailyStatVO {

    /** 日期，格式 yyyy-MM-dd。 */
    private String date;
    /** 该日答题总数（已评估）。 */
    private Long count;
    /** 该日正确率（百分比，保留一位小数）。 */
    private BigDecimal correctRate;
}
