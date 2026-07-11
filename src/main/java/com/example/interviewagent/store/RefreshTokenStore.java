package com.example.interviewagent.store;

import com.example.interviewagent.entity.RefreshToken;

import java.time.LocalDateTime;

/**
 * 刷新令牌存储抽象层。
 * <p>
 * 业务逻辑（{@code TokenService}）只依赖此接口，不感知底层是 MySQL 还是 Redis。
 * 当前默认实现为 {@code MysqlRefreshTokenStore}；后续接入 Redis 时，
 * 只需新增一个实现类并替换注入即可，无需改动任何业务代码。
 */
public interface RefreshTokenStore {

    /** 保存一枚新的 RT。 */
    void save(RefreshToken token);

    /** 按 jti 查找 RT，不存在返回 null。 */
    RefreshToken findByJti(String jti);

    /**
     * 该 RT 是否曾被正常轮换过（存在以其 jti 为 parent_jti 的后继）。
     * 用于区分「轮换取代」与「注销/踢人」两类失效。
     */
    boolean hasChild(String jti);

    /**
     * 按 jti 将 RT 标记为失效（仅当其当前仍有效时生效）。
     * @return 是否发生了状态变更（true 表示本次调用把它从有效改为失效）。
     */
    boolean revokeByJti(String jti, LocalDateTime revokedAt);

    /**
     * 吊销某用户全部仍有效的 RT。
     * @return 被吊销的 RT 数量。
     */
    int revokeAllByUserId(Long userId, LocalDateTime revokedAt);

    /**
     * 吊销某用户在指定设备上仍有效的 RT（登录挤旧，实现同设备单活跃会话）。
     * @return 被挤下线的 RT 数量。
     */
    int revokeActiveByUserAndDevice(Long userId, String deviceId, LocalDateTime revokedAt);
}
