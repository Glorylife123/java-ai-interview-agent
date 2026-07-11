package com.example.interviewagent.service.dto;

import com.example.interviewagent.entity.Tag;
import lombok.Data;

/** 批量查询题目标签时使用的关联投影。 */
@Data
public class QuestionTagRelation {

    private Long questionId;
    private Long tagId;
    private String tagName;
    private String tagCategory;

    public Tag toTag() {
        Tag tag = new Tag();
        tag.setId(tagId);
        tag.setName(tagName);
        tag.setCategory(tagCategory);
        return tag;
    }
}
