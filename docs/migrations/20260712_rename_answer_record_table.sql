-- 将答题记录表从 user_answer_record 重命名为 answer_record
--
-- 背景：代码层已把实体/Mapper/Service/VO 统一为 AnswerRecord 前缀，
--       并把 MyBatis XML 中的表名改为 answer_record，数据库表需同步改名。
--
-- 适用：尚未手动改名的环境（如 dev / prod）。已手动改名的环境无需再执行。
-- 注意：MySQL 的 RENAME TABLE 在「旧表不存在」或「新表已存在」时会报错。
--       执行前请确认当前库中存在 user_answer_record 且尚不存在 answer_record。

RENAME TABLE user_answer_record TO answer_record;

-- 验证：应能查到新表
SHOW TABLES LIKE 'answer_record';
