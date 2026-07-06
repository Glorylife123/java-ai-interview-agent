package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.WrongQuestion;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WrongQuestionMapper {

    @Insert("""
            INSERT INTO wrong_question (user_id, question_id, wrong_count, mastered)
            VALUES (#{userId}, #{questionId}, 1, 0)
            ON DUPLICATE KEY UPDATE
                wrong_count = wrong_count + 1,
                last_wrong_at = CURRENT_TIMESTAMP,
                mastered = 0,
                updated_at = CURRENT_TIMESTAMP
            """)
    int insertOrIncrease(WrongQuestion wrongQuestion);

    @Select("""
            SELECT *
            FROM wrong_question
            WHERE user_id = #{userId}
            ORDER BY mastered ASC, last_wrong_at DESC
            """)
    List<WrongQuestion> selectByUserId(Long userId);

    @Update("UPDATE wrong_question SET mastered = 1 WHERE id = #{id}")
    int markMastered(Long id);
}
