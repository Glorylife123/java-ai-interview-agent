-- 错题本表（若不存在则创建）。
-- 唯一键 (user_id, question_id) 保证同一用户同一题只有一条错题记录，
-- 供 ON DUPLICATE KEY UPDATE 做累加错误次数使用。
CREATE TABLE IF NOT EXISTS `wrong_question` (
    `id`            BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       BIGINT(20)    NOT NULL COMMENT '用户ID',
    `question_id`   BIGINT(20)    NOT NULL COMMENT '题目ID',
    `wrong_count`   INT(11)       DEFAULT 1 COMMENT '错误次数',
    `last_wrong_at` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '最近一次答错时间',
    `mastered`      TINYINT(1)    DEFAULT 0 COMMENT '是否已掌握（0未掌握，1已掌握）',
    `created_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
    KEY `idx_user_mastered` (`user_id`, `mastered`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '错题本';
