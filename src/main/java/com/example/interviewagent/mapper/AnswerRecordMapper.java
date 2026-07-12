package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.AnswerRecord;
import com.example.interviewagent.vo.AnswerRecordVO;
import com.example.interviewagent.vo.CategoryStatVO;
import com.example.interviewagent.vo.DailyStatVO;
import com.example.interviewagent.vo.PracticeStatVO;
import com.example.interviewagent.vo.WeakTagStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AnswerRecordMapper {

    int insert(AnswerRecord record);

    List<AnswerRecord> selectByUserId(@Param("userId") Long userId);

    /**
     * 按用户分页查询历史答题记录，联表 question 取题目标题，按 created_at 降序。
     */
    List<AnswerRecordVO> selectPageByUserId(@Param("userId") Long userId,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    /** 统计某用户的答题记录总数，用于分页 total。 */
    long countByUserId(@Param("userId") Long userId);

    // ==================== 练习统计 ====================

    /**
     * 总览统计：
     * 仅统计 is_correct 非 NULL 的记录；
     * avg_score 在 score 全为 NULL 时由数据库返回 NULL，由 Service 层兜底为 0。
     */
    PracticeStatVO selectOverviewStat(@Param("userId") Long userId);

    /**
     * 按分类（tag.category）统计答题情况。
     * question 表无 category 字段，需经 question_tag -> tag 关联后按 tag.category 分组。
     */
    List<CategoryStatVO> selectCategoryStats(@Param("userId") Long userId);

    /**
     * 薄弱标签统计：取错误次数最多的前 limit 个标签。
     * 仅聚合 is_correct = 0 的错误记录。
     */
    List<WeakTagStatVO> selectWeakTagStats(@Param("userId") Long userId,
                                            @Param("limit") Integer limit);

    /**
     * 最近 days 天的每日答题趋势。
     * 仅返回有答题记录的日期，缺失日期不补 0；按日期升序。
     */
    List<DailyStatVO> selectDailyStats(@Param("userId") Long userId,
                                        @Param("days") Integer days);

    /** 错题本题目数量（来自 wrong_question 表，与 answer_record 无关）。 */
    long countWrongBook(@Param("userId") Long userId);
}
