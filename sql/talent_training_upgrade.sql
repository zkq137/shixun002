-- 培训模块增量升级脚本
-- 在 talent_dataset.sql、talent_skill_tree.sql、talent_analysis.sql 之后执行。
USE `talent_db`;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `course_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category_name` VARCHAR(64) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训课程分类';

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'course' AND column_name = 'category_id');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE course ADD COLUMN category_id BIGINT DEFAULT NULL COMMENT ''课程分类'' AFTER course_type',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO course_category (category_name, description) VALUES
  ('通用能力', '沟通、协作、表达与职业素养'),
  ('管理与领导力', '团队管理、目标管理与组织能力'),
  ('专业技能', '岗位相关的专业方法与工具'),
  ('数据与技术', '数据分析、研发与数字化工具')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 学习路径的岗位适配与梯队培养目标；可重复执行。
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'learning_path' AND column_name = 'for_position');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE learning_path ADD COLUMN for_position VARCHAR(128) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'learning_path' AND column_name = 'development_goal');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE learning_path ADD COLUMN development_goal VARCHAR(255) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS training_algorithm (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  skill_weight DECIMAL(5,2) NOT NULL,
  position_weight DECIMAL(5,2) NOT NULL,
  goal_weight DECIMAL(5,2) NOT NULL,
  match_threshold DECIMAL(5,2) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_training_algorithm_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训匹配算法方案';
CREATE TABLE IF NOT EXISTS training_algorithm_lock (id INT NOT NULL PRIMARY KEY) ENGINE=InnoDB;
INSERT IGNORE INTO training_algorithm_lock (id) VALUES (1);
-- 初始规则权重是可编辑的产品默认值，不是模型训练结果；重跑不覆盖用户配置。
INSERT INTO training_algorithm (name, skill_weight, position_weight, goal_weight, match_threshold, enabled)
SELECT '均衡匹配（默认）', 60, 25, 15, 40, 1
WHERE NOT EXISTS (SELECT 1 FROM training_algorithm);

