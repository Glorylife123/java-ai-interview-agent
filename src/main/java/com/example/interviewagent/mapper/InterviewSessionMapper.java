package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.InterviewSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InterviewSessionMapper {

    int insert(InterviewSession session);

    InterviewSession selectById(@Param("id") Long id);

    int update(InterviewSession session);

    /** 查询用户所有会话，按创建时间倒序。 */
    List<InterviewSession> selectByUserId(@Param("userId") Long userId);

    /** 统计用户面试次数。 */
    int countByUserId(@Param("userId") Long userId);
}
