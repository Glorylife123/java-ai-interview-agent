package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.InterviewEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InterviewEvaluationMapper {

    int insert(InterviewEvaluation evaluation);

    InterviewEvaluation selectById(@Param("id") Long id);

    InterviewEvaluation selectByQuestionRecordId(@Param("questionRecordId") Long questionRecordId);

    /** 查询某会话下的全部评分，用于报告汇总。 */
    List<InterviewEvaluation> selectBySessionId(@Param("sessionId") Long sessionId);
}
