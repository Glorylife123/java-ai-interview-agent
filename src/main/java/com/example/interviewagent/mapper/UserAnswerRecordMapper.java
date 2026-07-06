package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.UserAnswerRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAnswerRecordMapper {

    @Insert("""
            INSERT INTO user_answer_record (
                user_id, question_id, user_answer, is_correct, score,
                time_cost_seconds, answer_source
            )
            VALUES (
                #{userId}, #{questionId}, #{userAnswer}, #{isCorrect}, #{score},
                #{timeCostSeconds}, #{answerSource}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAnswerRecord record);

    @Select("""
            SELECT *
            FROM user_answer_record
            WHERE user_id = #{userId}
            ORDER BY created_at DESC
            """)
    List<UserAnswerRecord> selectByUserId(Long userId);
}
