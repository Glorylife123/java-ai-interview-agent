package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.InterviewReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InterviewReportMapper {

    int insert(InterviewReport report);

    InterviewReport selectBySessionId(@Param("sessionId") Long sessionId);

    /** 用于报告重新生成时「先删后插」。 */
    int deleteBySessionId(@Param("sessionId") Long sessionId);
}
