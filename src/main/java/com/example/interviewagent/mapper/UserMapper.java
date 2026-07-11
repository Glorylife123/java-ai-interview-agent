package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User selectById(@Param("id") Long id);

    User selectByUsername(@Param("username") String username);

    List<User> selectPage(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    long countAll();

    int insert(User user);

    int updateById(User user);

    int logicDeleteById(@Param("id") Long id);
}
