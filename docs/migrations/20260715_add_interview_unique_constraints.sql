-- 为规则版模拟面试补充数据库级幂等约束。
-- 执行前若存在历史重复数据，ALTER TABLE 会失败，请先人工确认并清理，避免静默丢失答案。

ALTER TABLE `interview_question_record`
  ADD UNIQUE KEY `uk_interview_question_session_order` (`session_id`, `sort_order`),
  ADD UNIQUE KEY `uk_interview_question_session_question` (`session_id`, `question_id`);

ALTER TABLE `interview_answer`
  DROP INDEX `idx_question_record_id`,
  ADD UNIQUE KEY `uk_interview_answer_question_record` (`question_record_id`);

ALTER TABLE `interview_evaluation`
  DROP INDEX `idx_question_record_id`,
  ADD UNIQUE KEY `uk_interview_evaluation_question_record` (`question_record_id`),
  ADD UNIQUE KEY `uk_interview_evaluation_answer` (`answer_id`);
