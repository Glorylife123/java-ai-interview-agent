package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.Tag;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionTagMapper {

    @Insert("""
            INSERT IGNORE INTO question_tag (question_id, tag_id)
            VALUES (#{questionId}, #{tagId})
            """)
    int insertIgnore(@Param("questionId") Long questionId, @Param("tagId") Long tagId);

    @Select("""
            SELECT t.*
            FROM tag t
            INNER JOIN question_tag qt ON t.id = qt.tag_id
            WHERE qt.question_id = #{questionId} AND t.deleted = 0
            ORDER BY t.category, t.name
            """)
    List<Tag> selectTagsByQuestionId(Long questionId);
}
