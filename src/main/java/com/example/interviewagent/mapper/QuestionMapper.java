package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.Question;
import com.example.interviewagent.service.dto.QuestionCounters;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper {

    Question selectById(@Param("id") Long id);

    QuestionCounters selectCountersById(@Param("id") Long id);

    /**
     * 面试出题：随机取一道未出过、且（可选）指定难度的上架题目。
     * 排除逻辑删除、排除 excludeIds 中已出过的题；difficulty 为 null 时不按难度过滤。
     * 使用 ORDER BY RAND() LIMIT 1 随机选一题。
     */
    Question selectRandomForInterview(@Param("difficulty") Integer difficulty,
                                      @Param("excludeIds") List<Long> excludeIds);

    List<Question> selectPage(@Param("keyword") String keyword,
                              @Param("difficulty") Integer difficulty,
                              @Param("questionTypeValues") List<String> questionTypeValues,
                              @Param("tagId") Long tagId,
                              @Param("offset") Integer offset,
                              @Param("pageSize") Integer pageSize);

    long countPage(@Param("keyword") String keyword,
                   @Param("difficulty") Integer difficulty,
                   @Param("questionTypeValues") List<String> questionTypeValues,
                   @Param("tagId") Long tagId);

    int insert(Question question);

    int updateById(Question question);

    int logicDeleteById(@Param("id") Long id);

    int increaseViewCount(@Param("id") Long id);

    int increaseSubmitCount(@Param("id") Long id, @Param("isCorrect") Integer isCorrect);
}
