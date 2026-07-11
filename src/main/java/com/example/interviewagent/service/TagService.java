package com.example.interviewagent.service;

import com.example.interviewagent.entity.Tag;

import java.util.List;

public interface TagService {

    Tag create(Tag tag);

    Tag getById(Long id);

    List<Tag> listAll();

    Tag update(Long id, Tag tag);

    void delete(Long id);
}
