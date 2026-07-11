package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper {

    Question selectById(@Param("id") Long id);

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
