package com.example.interviewagent.service;

import com.example.interviewagent.entity.User;
import com.example.interviewagent.service.dto.IssuedTokens;

/**
 * 双令牌业务编排接口：签发、刷新（轮换）、注销、批量吊销。
 * 存储细节委托给 {@code RefreshTokenStore}，本层只承担业务规则与事务。
 */
public interface TokenService {

    /**
     * 登录成功后签发一对新的 AT + RT。
     *
     * @param user     已通过认证的用户
     * @param deviceId 设备标识（来自请求头 X-Device-Id）
     */
    IssuedTokens issueTokens(User user, String deviceId);

    /**
     * 刷新：校验旧 RT → 轮换 → 颁发新的 AT + RT。
     *
     * @param refreshTokenValue Cookie 中携带的旧 RT
     * @return 新的双令牌
     */
    IssuedTokens refresh(String refreshTokenValue);

    /**
     * 注销：将当前设备持有的 RT 标记为失效。
     *
     * @param refreshTokenValue Cookie 中携带的 RT
     */
    void logout(String refreshTokenValue);

    /**
     * 管理员踢人：吊销指定用户全部仍有效的 RT。
     */
    void revokeAllForUser(Long userId);
}
