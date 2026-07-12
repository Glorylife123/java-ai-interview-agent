package com.example.interviewagent.service;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.dto.AnswerSubmitDTO;
import com.example.interviewagent.vo.AnswerResultVO;
import com.example.interviewagent.vo.AnswerRecordVO;
import com.example.interviewagent.vo.CategoryStatVO;
import com.example.interviewagent.vo.DailyStatVO;
import com.example.interviewagent.vo.PracticeStatVO;
import com.example.interviewagent.vo.WeakTagStatVO;

import java.util.List;

public interface AnswerRecordService {

    /**
     * 提交答题记录（不接入 AI 判断）：仅保存用户答案，并回显标准答案与解析。
     * 不做正误判断、不评分、不写入错题本。
     */
    AnswerResultVO submitAnswer(Long userId, AnswerSubmitDTO dto);

    /** 分页查询当前用户的历史答题记录，按提交时间倒序。 */
    PageResult<AnswerRecordVO> pageRecords(Long userId, Integer pageNum, Integer pageSize);

    /** 练习总览统计：无答题记录时返回字段全 0 的默认 VO。 */
    PracticeStatVO getOverviewStat(Long userId);

    /** 按分类(tag.category)统计答题情况，无数据返回空列表。 */
    List<CategoryStatVO> getCategoryStats(Long userId);

    /** 薄弱标签统计(limit 默认 5)，无数据返回空列表。 */
    List<WeakTagStatVO> getWeakTagStats(Long userId, Integer limit);

    /** 每日练习趋势(days 默认 30)，无数据返回空列表。 */
    List<DailyStatVO> getDailyStats(Long userId, Integer days);
}
