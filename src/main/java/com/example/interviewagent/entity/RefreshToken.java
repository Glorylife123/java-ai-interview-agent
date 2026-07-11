package com.example.interviewagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 刷新令牌实体，对应表 refresh_tokens。
 */
@Data
public class RefreshToken {

    private Long id;
    /** 所属用户ID。 */
    private Long userId;
    /** 设备标识。 */
    private String deviceId;
    /** JWT ID，RT 的唯一标识。 */
    private String jti;
    /** 上一枚 RT 的 jti，形成轮换链；登录首枚为 null。 */
    private String parentJti;
    /** 过期时间。 */
    private LocalDateTime expiresAt;
    /** 失效时间，null 表示仍然有效。 */
    private LocalDateTime revokedAt;
    /** 创建时间。 */
    private LocalDateTime createdAt;
}
