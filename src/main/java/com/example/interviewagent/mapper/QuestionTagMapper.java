package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.Tag;
import com.example.interviewagent.service.dto.QuestionTagRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionTagMapper {

    int insertIgnore(@Param("questionId") Long questionId, @Param("tagId") Long tagId);

    List<Tag> selectTagsByQuestionId(@Param("questionId") Long questionId);

    /** 按当前页题目批量读取标签，避免列表展示产生 N+1 查询。 */
    List<QuestionTagRelation> selectRelationsByQuestionIds(@Param("questionIds") List<Long> questionIds);

    /** 删除一题当前所有关联，用于完整替换标签集合。 */
    int deleteByQuestionId(@Param("questionId") Long questionId);

    /** 批量插入经过校验、去重后的标签关联。 */
    int insertBatch(@Param("questionId") Long questionId, @Param("tagIds") List<Long> tagIds);
}
