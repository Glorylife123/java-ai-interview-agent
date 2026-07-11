package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.WrongQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WrongQuestionMapper {

    int insertOrIncrease(WrongQuestion wrongQuestion);

    List<WrongQuestion> selectByUserId(@Param("userId") Long userId);

    int markMastered(@Param("id") Long id);
}
