-- 规则版模拟面试模块建表脚本（无 AI）
-- 说明：全部使用 IF NOT EXISTS，重复执行安全。字符集统一 utf8mb4。

-- 面试会话表
CREATE TABLE IF NOT EXISTS `interview_session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `title` varchar(100) DEFAULT NULL COMMENT '面试标题（如"Java后端实习生面试"）',
  `position` varchar(50) NOT NULL COMMENT '岗位名称',
  `difficulty` varchar(20) NOT NULL COMMENT '难度：简单/中等/困难',
  `status` varchar(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED/IN_PROGRESS/FINISHED/CANCELLED',
  `total_question_count` int(11) NOT NULL DEFAULT 0 COMMENT '总题数',
  `current_question_index` int(11) NOT NULL DEFAULT 0 COMMENT '当前题目索引（从0开始）',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `ended_at` datetime DEFAULT NULL COMMENT '结束时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试会话';

-- 面试题目记录表（每次面试中出现的题目）
CREATE TABLE IF NOT EXISTS `interview_question_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint(20) NOT NULL COMMENT '面试会话ID',
  `question_id` bigint(20) NOT NULL COMMENT '题库题目ID（关联 question.id）',
  `question_content` text COMMENT '题目内容快照（当时题目的内容，防止题目后续被修改）',
  `competency` varchar(50) DEFAULT NULL COMMENT '考察能力（如"集合框架""并发编程"）',
  `difficulty` varchar(20) NOT NULL COMMENT '题目难度',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '题目顺序（从0开始）',
  `source_type` varchar(20) NOT NULL DEFAULT 'QUESTION_BANK' COMMENT '来源：QUESTION_BANK / RULE / AI',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  UNIQUE KEY `uk_interview_question_session_order` (`session_id`, `sort_order`),
  UNIQUE KEY `uk_interview_question_session_question` (`session_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题目记录';

-- 面试答案表
CREATE TABLE IF NOT EXISTS `interview_answer` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint(20) NOT NULL COMMENT '面试会话ID',
  `question_record_id` bigint(20) NOT NULL COMMENT '面试题目记录ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `answer_content` text COMMENT '用户回答内容',
  `duration_seconds` int(11) DEFAULT NULL COMMENT '答题耗时（秒）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  UNIQUE KEY `uk_interview_answer_question_record` (`question_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试答案';

-- 面试评分表
CREATE TABLE IF NOT EXISTS `interview_evaluation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint(20) NOT NULL COMMENT '面试会话ID',
  `question_record_id` bigint(20) NOT NULL COMMENT '面试题目记录ID',
  `answer_id` bigint(20) NOT NULL COMMENT '面试答案ID',
  `score` int(11) DEFAULT NULL COMMENT '得分（0-100）',
  `max_score` int(11) DEFAULT 100 COMMENT '满分',
  `level` varchar(20) DEFAULT NULL COMMENT '等级：优秀/良好/一般/较差',
  `matched_points` text COMMENT '命中得分点（JSON数组或逗号分隔）',
  `missing_points` text COMMENT '缺失得分点',
  `suggestion` text COMMENT '改进建议',
  `evaluator_type` varchar(20) NOT NULL DEFAULT 'RULE' COMMENT '评分者：RULE / AI / MANUAL',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  UNIQUE KEY `uk_interview_evaluation_question_record` (`question_record_id`),
  UNIQUE KEY `uk_interview_evaluation_answer` (`answer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试评分';

-- 面试报告表
CREATE TABLE IF NOT EXISTS `interview_report` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint(20) NOT NULL COMMENT '面试会话ID',
  `overall_score` int(11) DEFAULT NULL COMMENT '总体得分（0-100）',
  `summary` text COMMENT '总结评语',
  `dimension_scores_json` json DEFAULT NULL COMMENT '各维度得分（JSON格式）',
  `suggestions_json` json DEFAULT NULL COMMENT '学习建议（JSON数组）',
  `generator_type` varchar(20) NOT NULL DEFAULT 'RULE' COMMENT '生成者：RULE / AI',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试报告';
