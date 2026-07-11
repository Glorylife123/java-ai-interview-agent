---
name: verify
summary: 通过真实 Spring Boot API 和 MySQL 状态验证运行时行为
---

# 项目运行验证

1. 启动后端：`./mvnw spring-boot:run`。
2. 等待 `http://127.0.0.1:8080` 可访问；未认证访问 `/api/questions` 返回 401 表示服务已启动。
3. 使用 `curl` 驱动真实 API。管理员测试账号见 `docs/auth-dual-token-test.md`。
4. 验证双令牌时，为 `X-Device-Id` 使用 `verify-*` 专用值；通过 MySQL 查询 `refresh_tokens` 捕获状态证据。
5. 验证结束后删除所有 `device_id LIKE 'verify-%'` 的临时令牌记录，不影响浏览器的 `default` 会话。

登录挤旧核心检查：同一用户、同一设备连续登录两次后，仅最新 RT 满足 `revoked_at IS NULL AND expires_at > NOW()`；不同设备应各保留一枚有效 RT；旧 Cookie 调 `/api/auth/refresh` 应返回 401。
