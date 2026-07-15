package com.example.interviewagent.service;

import com.example.interviewagent.entity.User;

public interface AuthService {

    User register(String username, String password, String nickname);

    User login(String username, String password);

    /**
     * 修改密码：校验原密码后更新为新密码，并吊销该用户全部有效 Refresh Token，
     * 强制所有已登录会话（含攻击者可能持有的旧 RT）重新登录。
     *
     * @param userId      当前登录用户 ID（由拦截器写入的 authUserId）
     * @param oldPassword 原密码明文
     * @param newPassword 新密码明文
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
