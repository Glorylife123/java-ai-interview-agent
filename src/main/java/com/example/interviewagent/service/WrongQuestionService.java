package com.example.interviewagent.service;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.controller.dto.QuestionResponse;
import com.example.interviewagent.dto.WrongQuestionQueryDTO;
import com.example.interviewagent.entity.Question;
import com.example.interviewagent.entity.WrongQuestion;
import com.example.interviewagent.vo.WrongQuestionVO;

import java.util.List;

public interface WrongQuestionService {

    /** 自动加入错题本（错误次数累加），供后续答题模块在判定答错时调用。 */
    void addOrIncrease(Long userId, Long questionId);

    /** 插入或更新错题记录（与 addOrIncrease 等价的显式入口，命名贴合任务约定）。 */
    void addOrUpdateWrongQuestion(Long userId, Long questionId);

    /** 兼容旧调用名（同 addOrIncrease）。 */
    void increase(Long userId, Long questionId);

    /** 错题列表分页查询，带回题目信息。 */
    PageResult<WrongQuestionVO> pageWrongQuestions(Long userId, WrongQuestionQueryDTO query);

    /** 移除一条错题（物理删除）。 */
    void removeWrongQuestion(Long userId, Long questionId);

    /** 标记 / 取消掌握。mastered 为 null 视作设为已掌握。 */
    void markMastered(Long userId, Long questionId, Boolean mastered);

    /** 随机取一道错题对应的完整题目（含标签）。无错题返回 null。 */
    Question getRandomWrongQuestion(Long userId);

    /** 随机取一道错题的详情响应（含标签，供前端直接展示）。无错题返回 null。 */
    QuestionResponse getRandomWrongQuestionDetail(Long userId);

    /** 统计错题数量，供统计模块调用。 */
    int countWrongQuestions(Long userId);

    // ---------- 兼容旧接口 ----------

    WrongQuestion create(WrongQuestion wrongQuestion);

    List<WrongQuestion> listByUserId(Long userId);

    /** 按错题记录主键设为已掌握（旧入口，保留以防他处引用）。 */
    void markMastered(Long id);
}
