-- 面试题类型数据整理
--
-- 目标类型仅允许：八股题、场景题、项目题、算法题。
--
-- 修复记录（2026-07-12）：
--   1. 旧脚本 START TRANSACTION 之后把 COMMIT 注释掉了，整段执行时事务始终未提交，
--      会话结束即回滚，因此“数据库没更新”。本版在事务末尾显式 COMMIT。
--   2. 现网历史值仅有 SHORT_ANSWER 与 MULTIPLE 两种：
--        - SHORT_ANSWER → 八股题（已确认的安全别名，可批量转换）
--        - MULTIPLE     → 选择题不自动归类；经逐题人工审阅后见第 3 节显式分类
--
-- 执行方式：确认第 1 节盘点结果后，整段执行即可（已含 COMMIT）。
-- 如需先演练，可注释掉 COMMIT 与各 UPDATE，仅跑 SELECT。

-- =============================================================
-- 1. 执行前盘点：确认当前系统真实存在的类型值及数量
-- =============================================================
SELECT COALESCE(NULLIF(TRIM(question_type), ''), '<EMPTY>') AS question_type,
       COUNT(*) AS total
FROM question
WHERE deleted = 0
GROUP BY COALESCE(NULLIF(TRIM(question_type), ''), '<EMPTY>')
ORDER BY total DESC, question_type;

START TRANSACTION;

-- =============================================================
-- 2. 已确认的安全别名批量转换（空值/未知文本不在此处处理）
-- =============================================================
-- 八股类历史别名
UPDATE question
SET question_type = '八股题'
WHERE deleted = 0
  AND TRIM(question_type) IN ('SHORT_ANSWER', '简答题', '八股');

-- 场景类历史别名
UPDATE question
SET question_type = '场景题'
WHERE deleted = 0
  AND TRIM(question_type) IN ('SCENARIO', '场景');

-- 项目类历史别名
UPDATE question
SET question_type = '项目题'
WHERE deleted = 0
  AND TRIM(question_type) IN ('PROJECT', '项目');

-- 算法类历史别名
UPDATE question
SET question_type = '算法题'
WHERE deleted = 0
  AND TRIM(question_type) IN ('ALGORITHM', '算法');

-- =============================================================
-- 3. 人工分类结果：选择题不批量猜测，以下为逐题审阅后的显式决定
--    id=7  「以下哪些是 Java 的基本数据类型？」 基础概念 → 八股题
--    id=15 「以下关于 volatile 的说法哪些正确？」 基础概念 → 八股题
--    若之后新增选择题，请先运行第 4 节的“待分类”查询，再在此按 id 显式补充。
-- =============================================================
UPDATE question
SET question_type = '八股题'
WHERE deleted = 0
  AND id IN (7, 15);

-- =============================================================
-- 4. 提交前自检：正常情况下这条查询应返回 0 行
--    若有结果，说明仍有类型未落入四种规范值，需回到第 3 节人工分类
-- =============================================================
SELECT id, title, COALESCE(NULLIF(TRIM(question_type), ''), '<EMPTY>') AS question_type
FROM question
WHERE deleted = 0
  AND (question_type IS NULL
       OR TRIM(question_type) NOT IN ('八股题', '场景题', '项目题', '算法题'))
ORDER BY question_type, id;

-- =============================================================
-- 5. 提交（如第 4 节自检有异常，改为 ROLLBACK）
-- =============================================================
COMMIT;
-- ROLLBACK;

-- =============================================================
-- 6. 最终验证：正常情况下只应出现以下四种类型
-- =============================================================
SELECT question_type, COUNT(*) AS total
FROM question
WHERE deleted = 0
GROUP BY question_type
ORDER BY question_type;
