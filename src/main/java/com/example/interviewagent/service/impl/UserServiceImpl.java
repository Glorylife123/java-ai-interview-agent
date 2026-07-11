package com.example.interviewagent.service.impl;

import com.example.interviewagent.common.PageResult;
import com.example.interviewagent.entity.User;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.UserMapper;
import com.example.interviewagent.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public User create(User user) {
        if (user == null || !StringUtils.hasText(user.getUsername())) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (!StringUtils.hasText(user.getPasswordHash())) {
            throw new BusinessException(400, "密码哈希不能为空");
        }
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole("USER");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
        return getById(user.getId());
    }

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    @Override
    public PageResult<User> page(Integer pageNum, Integer pageSize) {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePageNum - 1) * safePageSize;
        List<User> records = userMapper.selectPage(offset, safePageSize);
        return new PageResult<>(userMapper.countAll(), safePageNum, safePageSize, records);
    }

    @Override
    public User update(Long id, User user) {
        user.setId(id);
        if (StringUtils.hasText(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        } else {
            user.setPasswordHash(null);
        }
        int rows = userMapper.updateById(user);
        if (rows == 0) {
            throw new BusinessException(404, "用户不存在或未修改");
        }
        return getById(id);
    }

    @Override
    public void delete(Long id) {
        if (userMapper.logicDeleteById(id) == 0) {
            throw new BusinessException(404, "用户不存在");
        }
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
