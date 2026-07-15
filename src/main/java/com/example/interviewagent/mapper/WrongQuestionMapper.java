package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.WrongQuestion;
import com.example.interviewagent.vo.WrongQuestionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WrongQuestionMapper {

    /** 内部调用版 UPSERT（供 Service 自动加入错题本使用）。 */
    int insertOrUpdate(@Param("userId") Long userId, @Param("questionId") Long questionId);

    // ==================== 错题本统计模块 ====================

    /** 分页查询错题列表，联表 question 取题目信息（避免 N+1）。mastered 为 null 查全部。 */
    List<WrongQuestionVO> selectPageWithQuestion(@Param("userId") Long userId,
                                                  @Param("mastered") Integer mastered,
                                                  @Param("offset") int offset,
                                                  @Param("pageSize") int pageSize);

    /** 分页 total：与 selectPageWithQuestion 等条件对齐。 */
    long countPageWithQuestion(@Param("userId") Long userId,
                                @Param("mastered") Integer mastered);

    /** 按 (userId, questionId) 查询错题记录，用于判断是否已存在。 */
    WrongQuestion selectByUserIdAndQuestionId(@Param("userId") Long userId,
                                                @Param("questionId") Long questionId);

    /** 物理删除一条错题（本模块不使用逻辑删除）。 */
    int deleteByUserIdAndQuestionId(@Param("userId") Long userId,
                                     @Param("questionId") Long questionId);

    /** 标记/取消掌握状态。 */
    int updateMastered(@Param("userId") Long userId,
                       @Param("questionId") Long questionId,
                       @Param("mastered") Integer mastered);

    /** 随机获取一道错题的 question_id（用于错题再练）。 */
    Long selectRandomWrongQuestionId(@Param("userId") Long userId);

    /** 统计某用户错题总数。 */
    int countByUserId(@Param("userId") Long userId);
}
