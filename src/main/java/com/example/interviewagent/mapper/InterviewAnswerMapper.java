package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.InterviewAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InterviewAnswerMapper {

    int insert(InterviewAnswer answer);

    InterviewAnswer selectById(@Param("id") Long id);

    /** 根据题目记录ID查询答案，用于防止重复回答（一题一答）。 */
    InterviewAnswer selectByQuestionRecordId(@Param("questionRecordId") Long questionRecordId);
}
