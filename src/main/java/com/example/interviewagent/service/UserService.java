package com.example.interviewagent.service;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.entity.User;

public interface UserService {

    User create(User user);

    User getById(Long id);

    PageResult<User> page(Integer pageNum, Integer pageSize);

    User update(Long id, User user);

    void delete(Long id);
}
