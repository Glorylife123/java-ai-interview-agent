package com.example.interviewagent.service.impl;

import com.example.interviewagent.entity.Tag;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.TagMapper;
import com.example.interviewagent.redis.QuestionRedisService;
import com.example.interviewagent.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final QuestionRedisService questionRedisService;

    @Override
    public Tag create(Tag tag) {
        if (tag == null || !StringUtils.hasText(tag.getName())) {
            throw new BusinessException(400, "标签名不能为空");
        }
        tagMapper.insert(tag);
        return getById(tag.getId());
    }

    @Override
    public Tag getById(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }
        return tag;
    }

    @Override
    public List<Tag> listAll() {
        return tagMapper.selectAll();
    }

    @Override
    public Tag update(Long id, Tag tag) {
        tag.setId(id);
        int rows = tagMapper.updateById(tag);
        if (rows == 0) {
            throw new BusinessException(404, "标签不存在或未修改");
        }
        Tag updated = getById(id);
        questionRedisService.evictAllDetails();
        return updated;
    }

    @Override
    public void delete(Long id) {
        if (tagMapper.logicDeleteById(id) == 0) {
            throw new BusinessException(404, "标签不存在");
        }
        questionRedisService.evictAllDetails();
    }
}
