package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper {

    Tag selectById(@Param("id") Long id);

    List<Tag> selectAll();

    int insert(Tag tag);

    int updateById(Tag tag);

    int logicDeleteById(@Param("id") Long id);
}
