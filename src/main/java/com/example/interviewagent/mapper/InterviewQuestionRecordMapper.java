package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.InterviewQuestionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InterviewQuestionRecordMapper {

    int insert(InterviewQuestionRecord record);

    InterviewQuestionRecord selectById(@Param("id") Long id);

    /** 查询某会话下的全部题目记录，按 sort_order 升序。 */
    List<InterviewQuestionRecord> selectBySessionId(@Param("sessionId") Long sessionId);

    int countBySessionId(@Param("sessionId") Long sessionId);

    /** 根据会话与顺序号查询单条题目记录。 */
    InterviewQuestionRecord selectBySessionIdAndOrder(@Param("sessionId") Long sessionId,
                                                      @Param("sortOrder") Integer sortOrder);
}
