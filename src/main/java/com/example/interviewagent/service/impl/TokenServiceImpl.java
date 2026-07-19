package com.example.interviewagent.service.impl;

import com.example.interviewagent.config.JwtProperties;
import com.example.interviewagent.entity.RefreshToken;
import com.example.interviewagent.entity.User;
import com.example.interviewagent.exception.BusinessException;
import com.example.interviewagent.redis.AuthTokenRedisService;
import com.example.interviewagent.security.JwtTokenProvider;
import com.example.interviewagent.security.SignedRefreshToken;
import com.example.interviewagent.service.TokenService;
import com.example.interviewagent.service.UserService;
import com.example.interviewagent.service.dto.IssuedTokens;
import com.example.interviewagent.store.RefreshTokenStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 双令牌业务实现。核心为 {@link #refresh(String)} 的「轮换 + 宽限期」逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenStore refreshTokenStore;
    private final UserService userService;
    private final AuthTokenRedisService authTokenRedisService;

    @Override
    @Transactional
    public IssuedTokens issueTokens(User user, String deviceId) {
        // 单设备单活跃会话：登录时先挤掉该用户在同一设备上此前仍有效的 RT，
        // 避免每次重新登录都新增一枚有效根令牌，导致有效 RT 无限累积。
        int evicted = refreshTokenStore.revokeActiveByUserAndDevice(user.getId(), deviceId, LocalDateTime.now());
        if (evicted > 0) {
            log.info("登录挤旧：吊销该用户同设备旧有效 RT。userId={}, deviceId={}, 数量={}",
                    user.getId(), deviceId, evicted);
        }
        // 登录首枚 RT 没有父令牌，parentJti 传 null
        return issueInternal(user, deviceId, null);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public IssuedTokens refresh(String refreshTokenValue) {
        // ---------- 1. 校验签名与过期时间 ----------
        Claims claims;
        try {
            claims = jwtTokenProvider.parseAndValidate(refreshTokenValue, JwtTokenProvider.TYPE_REFRESH);
        } catch (JwtException e) {
            // 签名不合法 / 已过期 / 类型不符
            throw new BusinessException(401, "Refresh Token 无效或已过期");
        }
        String jti = claims.getId();
        Long userId = Long.valueOf(claims.getSubject());

        // ---------- 2. 查库确认该 RT 确实由本系统签发且未被清理 ----------
        RefreshToken stored = refreshTokenStore.findByJti(jti);
        if (stored == null) {
            throw new BusinessException(401, "Refresh Token 不存在");
        }

        LocalDateTime now = LocalDateTime.now();

        // ---------- 3. 校验 revoked_at：区分「轮换取代」与「注销/踢人」，并处理宽限期与复用检测 ----------
        // 注意：本方法 @Transactional(noRollbackFor = BusinessException.class)，
        // 因此下方复用检测里的「链级吊销」写入会随 401 一起提交，不会被回滚。
        if (stored.getRevokedAt() != null) {
            // 该 RT 是否曾被正常轮换过（存在后继）。宽限期仅对「被轮换取代」的 RT 有意义；
            // 因注销 / 管理员踢人而失效的 RT 没有后继，不享受宽限期，必须立即拒绝，杜绝 30s 撤销绕过。
            boolean supersededByRotation = refreshTokenStore.hasChild(jti);
            long secondsSinceRevoked = Duration.between(stored.getRevokedAt(), now).getSeconds();
            boolean inGrace = secondsSinceRevoked <= jwtProperties.getGraceSeconds();

            if (supersededByRotation && inGrace) {
                // 已轮换 + 宽限期内：极可能是客户端因网络抖动对同一枚 RT 重复发起刷新，
                // 视为合法重传，放行并颁发新令牌，仅记录 Warn 便于观测。
                log.warn("RT 已轮换但在宽限期内（{}s <= {}s），判定为网络重传，放行。userId={}, jti={}",
                        secondsSinceRevoked, jwtProperties.getGraceSeconds(), userId, jti);
            } else if (supersededByRotation) {
                // 已轮换 + 超出宽限期：判定为令牌复用 / 重放（很可能 RT 被窃取）。
                // 无法区分攻击者与真实用户，按最佳实践吊销该用户「整条链」全部有效 RT，强制全端重登。
                log.warn("检测到已轮换 RT 被复用且超出宽限期（{}s > {}s），吊销该用户全部有效 RT。userId={}, jti={}",
                        secondsSinceRevoked, jwtProperties.getGraceSeconds(), userId, jti);
                int revoked = refreshTokenStore.revokeAllByUserId(userId, now);
                authTokenRedisService.evictAccessToken(userId);
                log.warn("复用检测触发链级吊销完成。userId={}, 吊销数量={}", userId, revoked);
                throw new BusinessException(401, "检测到 Refresh Token 复用，已登出全部会话，请重新登录");
            } else {
                // 未被轮换却已失效：来自注销 / 管理员踢人，直接拒绝，不放行。
                log.warn("已失效（注销 / 被踢）的 RT 尝试刷新，拒绝。userId={}, jti={}", userId, jti);
                throw new BusinessException(401, "Refresh Token 已失效");
            }
        }

        // ---------- 4. 确认用户仍存在且未被禁用（顺带拿到最新 role 写入新 AT） ----------
        User user = userService.getById(userId);
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // ---------- 5. 轮换：旧 RT 置失效 + 颁发新 RT（parent_jti 指向旧 jti） ----------
        // revokeByJti 内部条件 revoked_at IS NULL，宽限期场景下旧记录已失效会返回 false，不影响流程。
        refreshTokenStore.revokeByJti(jti, now);
        return issueInternal(user, stored.getDeviceId(), jti);
    }

    @Override
    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            // 无 RT 可注销，视为幂等成功，直接返回
            return;
        }
        Claims claims;
        try {
            // 注销允许 RT 已过期：仍需取出 jti 去吊销数据库记录
            claims = jwtTokenProvider.parseAllowExpired(refreshTokenValue, JwtTokenProvider.TYPE_REFRESH);
        } catch (JwtException e) {
            // 签名非法的 RT 无需处理，幂等返回
            log.warn("注销时收到无法解析的 RT，已忽略。原因：{}", e.getMessage());
            return;
        }
        String jti = claims.getId();
        Long userId = Long.valueOf(claims.getSubject());
        boolean changed = refreshTokenStore.revokeByJti(jti, LocalDateTime.now());
        authTokenRedisService.evictAccessToken(userId);
        log.info("用户注销，吊销当前设备 RT。jti={}, 是否变更={}", jti, changed);
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        // 校验用户存在（不存在会抛 404）
        userService.getById(userId);
        int count = refreshTokenStore.revokeAllByUserId(userId, LocalDateTime.now());
        authTokenRedisService.evictAccessToken(userId);
        log.info("管理员踢人，吊销用户全部有效 RT。userId={}, 数量={}", userId, count);
    }

    /**
     * 内部签发：生成新 jti，签发 RT 并落库，再签发 AT，最终打包返回。
     *
     * @param parentJti 上一枚 RT 的 jti（登录为 null，刷新为旧 jti）
     */
    private IssuedTokens issueInternal(User user, String deviceId, String parentJti) {
        String newJti = UUID.randomUUID().toString();
        SignedRefreshToken signedRt = jwtTokenProvider.createRefreshToken(user.getId(), deviceId, newJti);

        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getId());
        entity.setDeviceId(deviceId);
        entity.setJti(newJti);
        entity.setParentJti(parentJti);
        entity.setExpiresAt(signedRt.expiresAt());
        refreshTokenStore.save(entity);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        authTokenRedisService.cacheAccessToken(
                user.getId(), accessToken, Duration.ofSeconds(jwtProperties.getAccessTokenTtlSeconds()));
        return new IssuedTokens(
                accessToken,
                jwtProperties.getAccessTokenTtlSeconds(),
                signedRt.token(),
                jwtProperties.getRefreshTokenTtlSeconds()
        );
    }
}
