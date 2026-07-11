package com.example.interviewagent.mapper;

import com.example.interviewagent.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 刷新令牌 DAO。仅负责与数据库交互，不含任何业务逻辑。
 */
@Mapper
public interface RefreshTokenMapper {

    /** 插入一枚新的 RT 记录。 */
    int insert(RefreshToken token);

    /** 按 jti 查询 RT 记录。 */
    RefreshToken selectByJti(@Param("jti") String jti);

    /**
     * 是否存在以该 jti 为 parent_jti 的后继 RT（即该 RT 曾被正常轮换过）。
     * 用于区分「被轮换取代而失效」与「被注销/踢人而失效」。
     * @return 存在返回 &gt;0。
     */
    int countChildren(@Param("jti") String jti);

    /**
     * 按 jti 将 RT 标记为失效（仅当当前 revoked_at 为 NULL 时更新）。
     * @return 影响行数，0 表示该记录不存在或已被失效。
     */
    int revokeByJti(@Param("jti") String jti, @Param("revokedAt") LocalDateTime revokedAt);

    /**
     * 将某用户所有仍有效（revoked_at IS NULL）的 RT 批量标记为失效。
     * @return 影响行数（被吊销的令牌数）。
     */
    int revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);

    /**
     * 将某用户在指定设备上仍有效（revoked_at IS NULL）的 RT 批量标记为失效。
     * 用于登录时「挤旧」，实现同一设备单活跃会话，避免有效 RT 无限累积。
     * @return 影响行数（被挤下线的令牌数）。
     */
    int revokeActiveByUserAndDevice(@Param("userId") Long userId,
                                    @Param("deviceId") String deviceId,
                                    @Param("revokedAt") LocalDateTime revokedAt);
}
