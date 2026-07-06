package com.example.interviewagent.service;

import com.example.interviewagent.entity.Tag;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;

    public Tag create(Tag tag) {
        if (tag == null || !StringUtils.hasText(tag.getName())) {
            throw new BusinessException(400, "标签名不能为空");
        }
        tagMapper.insert(tag);
        return getById(tag.getId());
    }

    public Tag getById(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }
        return tag;
    }

    public List<Tag> listAll() {
        return tagMapper.selectAll();
    }

    public Tag update(Long id, Tag tag) {
        tag.setId(id);
        int rows = tagMapper.updateById(tag);
        if (rows == 0) {
            throw new BusinessException(404, "标签不存在或未修改");
        }
        return getById(id);
    }

    public void delete(Long id) {
        if (tagMapper.logicDeleteById(id) == 0) {
            throw new BusinessException(404, "标签不存在");
        }
    }
}
