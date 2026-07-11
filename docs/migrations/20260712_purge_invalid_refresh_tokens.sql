-- 清理无效 / 过期的刷新令牌记录
--
-- 背景：双令牌采用轮换制，每次登录/刷新都会新增一枚 RT，旧的被置为
--       revoked_at（已吊销）但不会自动删除；过期的 RT 也长期滞留。
--       本脚本删除所有「已吊销」或「已过期」的记录，只保留当前有效的 RT。
--
-- 判定：有效 = revoked_at IS NULL AND expires_at > NOW()
--       其余（已吊销 或 已过期）即为无效，可安全删除，不影响任何登录状态。

START TRANSACTION;

DELETE FROM refresh_tokens
WHERE revoked_at IS NOT NULL
   OR expires_at <= NOW();

COMMIT;

-- 验证：剩余记录应全部为有效 RT
SELECT COUNT(*) AS remaining,
       SUM(revoked_at IS NULL AND expires_at > NOW()) AS still_valid
FROM refresh_tokens;
