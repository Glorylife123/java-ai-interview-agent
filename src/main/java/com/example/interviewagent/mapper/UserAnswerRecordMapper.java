package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.UserAnswerRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserAnswerRecordMapper {

    int insert(UserAnswerRecord record);

    List<UserAnswerRecord> selectByUserId(@Param("userId") Long userId);
}
