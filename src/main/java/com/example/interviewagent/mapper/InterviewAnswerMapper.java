package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.InterviewAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InterviewAnswerMapper {

    int insert(InterviewAnswer answer);

    InterviewAnswer selectById(@Param("id") Long id);

    /** 根据题目记录ID查询答案，用于防止重复回答（一题一答）。 */
    InterviewAnswer selectByQuestionRecordId(@Param("questionRecordId") Long questionRecordId);

    /** 查询某会话下的全部答案，用于「查看我的答题情况」。 */
    List<InterviewAnswer> selectBySessionId(@Param("sessionId") Long sessionId);
}
