package com.example.interviewagent.store.impl;

import com.example.interviewagent.entity.RefreshToken;
import com.example.interviewagent.mapper.RefreshTokenMapper;
import com.example.interviewagent.store.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 刷新令牌存储的 MySQL 实现。
 * <p>
 * 仅做存储访问的转发，业务规则（轮换、宽限期、事务）由上层 {@code TokenService} 承担。
 * 后续若切换到 Redis，只需新增一个实现 {@link RefreshTokenStore} 的类替换本类即可。
 */
@Component
@RequiredArgsConstructor
public class MysqlRefreshTokenStore implements RefreshTokenStore {

    private final RefreshTokenMapper refreshTokenMapper;

    @Override
    public void save(RefreshToken token) {
        refreshTokenMapper.insert(token);
    }

    @Override
    public RefreshToken findByJti(String jti) {
        return refreshTokenMapper.selectByJti(jti);
    }

    @Override
    public boolean hasChild(String jti) {
        return refreshTokenMapper.countChildren(jti) > 0;
    }

    @Override
    public boolean revokeByJti(String jti, LocalDateTime revokedAt) {
        // Mapper 内部条件为 revoked_at IS NULL，返回受影响行数；>0 表示确实由有效改为失效
        return refreshTokenMapper.revokeByJti(jti, revokedAt) > 0;
    }

    @Override
    public int revokeAllByUserId(Long userId, LocalDateTime revokedAt) {
        return refreshTokenMapper.revokeAllByUserId(userId, revokedAt);
    }

    @Override
    public int revokeActiveByUserAndDevice(Long userId, String deviceId, LocalDateTime revokedAt) {
        return refreshTokenMapper.revokeActiveByUserAndDevice(userId, deviceId, revokedAt);
    }
}
