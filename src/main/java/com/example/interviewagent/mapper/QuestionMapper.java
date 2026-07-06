package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.Question;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface QuestionMapper {

    @Select("SELECT * FROM question WHERE id = #{id} AND deleted = 0")
    Question selectById(Long id);

    @Select("""
            <script>
            SELECT DISTINCT q.*
            FROM question q
            <if test="tagId != null">
                INNER JOIN question_tag qt ON q.id = qt.question_id
            </if>
            WHERE q.deleted = 0
            <if test="keyword != null and keyword != ''">
                AND (q.title LIKE CONCAT('%', #{keyword}, '%') OR q.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="difficulty != null">
                AND q.difficulty = #{difficulty}
            </if>
            <if test="tagId != null">
                AND qt.tag_id = #{tagId}
            </if>
            ORDER BY q.created_at DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<Question> selectPage(@Param("keyword") String keyword,
                              @Param("difficulty") Integer difficulty,
                              @Param("tagId") Long tagId,
                              @Param("offset") Integer offset,
                              @Param("pageSize") Integer pageSize);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT q.id)
            FROM question q
            <if test="tagId != null">
                INNER JOIN question_tag qt ON q.id = qt.question_id
            </if>
            WHERE q.deleted = 0
            <if test="keyword != null and keyword != ''">
                AND (q.title LIKE CONCAT('%', #{keyword}, '%') OR q.content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="difficulty != null">
                AND q.difficulty = #{difficulty}
            </if>
            <if test="tagId != null">
                AND qt.tag_id = #{tagId}
            </if>
            </script>
            """)
    long countPage(@Param("keyword") String keyword,
                   @Param("difficulty") Integer difficulty,
                   @Param("tagId") Long tagId);

    @Insert("""
            INSERT INTO question (
                title, content, question_type, difficulty, answer, answer_analysis,
                source, view_count, submit_count, correct_count, status, created_by
            )
            VALUES (
                #{title}, #{content}, #{questionType}, #{difficulty}, #{answer}, #{answerAnalysis},
                #{source}, #{viewCount}, #{submitCount}, #{correctCount}, #{status}, #{createdBy}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Question question);

    @Update("""
            <script>
            UPDATE question
            <set>
                <if test="title != null">title = #{title},</if>
                <if test="content != null">content = #{content},</if>
                <if test="questionType != null">question_type = #{questionType},</if>
                <if test="difficulty != null">difficulty = #{difficulty},</if>
                <if test="answer != null">answer = #{answer},</if>
                <if test="answerAnalysis != null">answer_analysis = #{answerAnalysis},</if>
                <if test="source != null">source = #{source},</if>
                <if test="status != null">status = #{status},</if>
            </set>
            WHERE id = #{id} AND deleted = 0
            </script>
            """)
    int updateById(Question question);

    @Update("UPDATE question SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int logicDeleteById(Long id);

    @Update("UPDATE question SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int increaseViewCount(Long id);

    @Update("""
            UPDATE question
            SET submit_count = submit_count + 1,
                correct_count = correct_count + CASE WHEN #{isCorrect} = 1 THEN 1 ELSE 0 END
            WHERE id = #{id} AND deleted = 0
            """)
    int increaseSubmitCount(@Param("id") Long id, @Param("isCorrect") Integer isCorrect);
}
