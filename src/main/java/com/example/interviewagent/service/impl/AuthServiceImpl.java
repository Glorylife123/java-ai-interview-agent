package com.example.interviewagent.service.impl;

import com.example.interviewagent.entity.User;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.mapper.UserMapper;
import com.example.interviewagent.service.AuthService;
import com.example.interviewagent.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    public User register(String username, String password, String nickname) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw new BusinessException(400, "密码不能少于6位");
        }
        if (!StringUtils.hasText(nickname)) {
            nickname = username;
        }
        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        return userMapper.selectById(user.getId());
    }

    @Override
    public User login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException(400, "用户名和密码不能为空");
        }

        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        // Return user without password hash
        user.setPasswordHash(null);
        return user;
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException(400, "原密码和新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException(400, "新密码不能少于6位");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            // 原密码不匹配属于表单校验错误而非 AT 鉴权失败；返回 400，
            // 避免前端全局 401 拦截器误触发 Refresh Token 刷新。
            throw new BusinessException(400, "原密码错误");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }

        // 1. 仅更新 password_hash（updateById 为动态部分更新）
        User patch = new User();
        patch.setId(userId);
        patch.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(patch);

        // 2. 关键：改密后吊销该用户全部有效 RT，强制所有会话（含旧密码登录的攻击者）重新登录。
        //    与上面的更新同处一个事务，任一失败则整体回滚。
        tokenService.revokeAllForUser(userId);
    }
}
