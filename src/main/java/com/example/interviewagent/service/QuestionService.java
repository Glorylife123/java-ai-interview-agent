package com.example.interviewagent.service;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.controller.dto.QuestionResponse;
import com.example.interviewagent.controller.dto.QuestionUpsertRequest;
import com.example.interviewagent.controller.dto.HotQuestionResponse;
import com.example.interviewagent.entity.Question;
import com.example.interviewagent.entity.Tag;

import java.util.List;

public interface QuestionService {

    QuestionResponse create(QuestionUpsertRequest request);

    /** 内部业务校验仍使用持久化实体，不直接对外返回。 */
    Question getById(Long id);

    QuestionResponse viewDetail(Long id);

    List<HotQuestionResponse> listHot(Integer limit);

    PageResult<QuestionResponse> page(String keyword, Integer difficulty, String questionType,
                                      Long tagId, Integer pageNum, Integer pageSize);

    QuestionResponse update(Long id, QuestionUpsertRequest request);

    void delete(Long id);

    /** 兼容已有的增量绑定接口；管理端编辑器改用新增/编辑请求中的 tagIds 完整替换。 */
    void bindTag(Long questionId, Long tagId);

    List<Tag> listTags(Long questionId);

    void increaseSubmitCount(Long questionId, Integer isCorrect);
}
