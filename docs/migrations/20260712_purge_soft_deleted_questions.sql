-- 物理清理已逻辑删除（deleted=1）的题目脏数据
--
-- 背景：题库采用逻辑删除，前端删除只把 deleted 置 1，行仍留在库里。
--       本脚本把这些历史脏行连同其标签关联一并物理删除。
--
-- 安全前提（执行前已核对）：
--   deleted=1 的题目在 wrong_question、answer_record 中均无引用；
--   仅在 question_tag 中存在关联，需先删关联再删题目，避免留下孤儿数据。

START TRANSACTION;

-- 1. 先删已删除题目的标签关联
DELETE qt
FROM question_tag qt
JOIN question q ON q.id = qt.question_id
WHERE q.deleted = 1;

-- 2. 再物理删除逻辑删除的题目
DELETE FROM question
WHERE deleted = 1;

COMMIT;

-- 验证：库中应不再有 deleted=1 的题目
SELECT COUNT(*) AS remaining_soft_deleted
FROM question
WHERE deleted = 1;
