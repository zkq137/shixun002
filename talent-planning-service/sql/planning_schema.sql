-- 梯队规划模块增量脚本。可重复执行，不写入员工、技能、绩效或潜力公共数据。
CREATE DATABASE IF NOT EXISTS `talent_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `talent_db`;

CREATE TABLE IF NOT EXISTS `pos_skill_require` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `position_id` BIGINT NOT NULL COMMENT '岗位ID',
  `skill_id` BIGINT NOT NULL COMMENT '技能ID',
  `requirement_type` VARCHAR(16) NOT NULL DEFAULT '核心' COMMENT '要求类型',
  `required_level` VARCHAR(16) DEFAULT NULL COMMENT '要求等级',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_position_skill` (`position_id`, `skill_id`),
  KEY `idx_skill` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位技能要求';

DELIMITER $$
DROP PROCEDURE IF EXISTS `planning_add_column`$$
CREATE PROCEDURE `planning_add_column`(
  IN table_name_value VARCHAR(64),
  IN column_name_value VARCHAR(64),
  IN column_definition_value VARCHAR(1000)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name_value
      AND COLUMN_NAME = column_name_value
  ) THEN
    SET @planning_sql = CONCAT('ALTER TABLE `', table_name_value, '` ADD COLUMN `',
      column_name_value, '` ', column_definition_value);
    PREPARE planning_stmt FROM @planning_sql;
    EXECUTE planning_stmt;
    DEALLOCATE PREPARE planning_stmt;
  END IF;
END$$

DROP PROCEDURE IF EXISTS `planning_add_index`$$
CREATE PROCEDURE `planning_add_index`(
  IN table_name_value VARCHAR(64),
  IN index_name_value VARCHAR(64),
  IN index_definition_value VARCHAR(1000)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name_value
      AND INDEX_NAME = index_name_value
  ) THEN
    SET @planning_sql = CONCAT('ALTER TABLE `', table_name_value, '` ADD ', index_definition_value);
    PREPARE planning_stmt FROM @planning_sql;
    EXECUTE planning_stmt;
    DEALLOCATE PREPARE planning_stmt;
  END IF;
END$$
DELIMITER ;

CALL planning_add_column('pos_succession_candidate', 'skill_score',
  'DECIMAL(5,2) DEFAULT NULL COMMENT ''技能匹配分''');
CALL planning_add_column('pos_succession_candidate', 'performance_score',
  'DECIMAL(5,2) DEFAULT NULL COMMENT ''绩效分''');
CALL planning_add_column('pos_succession_candidate', 'potential_score',
  'DECIMAL(5,2) DEFAULT NULL COMMENT ''潜力分''');
CALL planning_add_column('pos_succession_candidate', 'experience_score',
  'DECIMAL(5,2) DEFAULT NULL COMMENT ''经验分''');
CALL planning_add_column('pos_succession_candidate', 'readiness',
  'VARCHAR(32) DEFAULT NULL COMMENT ''准备度''');
CALL planning_add_column('pos_succession_candidate', 'preparation_months',
  'INT DEFAULT NULL COMMENT ''预计准备月数''');
CALL planning_add_column('pos_succession_candidate', 'missing_skills',
  'TEXT DEFAULT NULL COMMENT ''缺失技能''');
CALL planning_add_column('pos_succession_candidate', 'recommendation_reason',
  'VARCHAR(500) DEFAULT NULL COMMENT ''推荐理由''');
CALL planning_add_column('pos_succession_candidate', 'data_warnings',
  'VARCHAR(500) DEFAULT NULL COMMENT ''数据完整性提示''');
CALL planning_add_column('pos_succession_candidate', 'calculated_at',
  'DATETIME DEFAULT NULL COMMENT ''计算时间''');

CALL planning_add_column('talent_pool', 'ready_now_count',
  'INT NOT NULL DEFAULT 0 COMMENT ''可立即继任人数''');
CALL planning_add_column('talent_pool', 'ready_one_year_count',
  'INT NOT NULL DEFAULT 0 COMMENT ''一年内可继任人数''');
CALL planning_add_column('talent_pool', 'updated_at',
  'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');

CALL planning_add_column('pos_risk', 'incumbent_count',
  'INT NOT NULL DEFAULT 0 COMMENT ''在岗人数''');
CALL planning_add_column('pos_risk', 'successor_count',
  'INT NOT NULL DEFAULT 0 COMMENT ''合格继任人数''');
CALL planning_add_column('pos_risk', 'ready_now_count',
  'INT NOT NULL DEFAULT 0 COMMENT ''可立即继任人数''');
CALL planning_add_column('pos_risk', 'high_risk_employee_count',
  'INT NOT NULL DEFAULT 0 COMMENT ''高流失风险人数''');
CALL planning_add_column('pos_risk', 'skill_coverage_rate',
  'DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT ''技能覆盖率''');

CALL planning_add_column('resign_warning_record', 'handler',
  'VARCHAR(64) DEFAULT NULL COMMENT ''处理人''');
CALL planning_add_column('resign_warning_record', 'handle_remark',
  'VARCHAR(500) DEFAULT NULL COMMENT ''处理备注''');
CALL planning_add_column('resign_warning_record', 'handled_at',
  'DATETIME DEFAULT NULL COMMENT ''处理时间''');

-- 先清理历史重复快照，再建立业务唯一约束。
DELETE duplicate_row FROM pos_succession_candidate duplicate_row
JOIN pos_succession_candidate keep_row
  ON duplicate_row.position_id = keep_row.position_id
 AND duplicate_row.employee_id = keep_row.employee_id
 AND duplicate_row.id > keep_row.id;
DELETE duplicate_row FROM talent_pool duplicate_row
JOIN talent_pool keep_row
  ON duplicate_row.level <=> keep_row.level
 AND duplicate_row.id > keep_row.id;
DELETE duplicate_row FROM pos_risk duplicate_row
JOIN pos_risk keep_row
  ON duplicate_row.position_id = keep_row.position_id
 AND duplicate_row.id > keep_row.id;

CALL planning_add_index('pos_succession_candidate', 'uk_position_employee',
  'UNIQUE KEY `uk_position_employee` (`position_id`, `employee_id`)');
CALL planning_add_index('pos_succession_candidate', 'idx_readiness',
  'KEY `idx_readiness` (`readiness`)');
CALL planning_add_index('talent_pool', 'uk_talent_pool_level',
  'UNIQUE KEY `uk_talent_pool_level` (`level`)');
CALL planning_add_index('pos_risk', 'uk_position_risk',
  'UNIQUE KEY `uk_position_risk` (`position_id`)');
CALL planning_add_index('resign_warning_record', 'idx_warning_filter',
  'KEY `idx_warning_filter` (`warning_level`, `handle_status`, `warning_time`)');

DROP PROCEDURE IF EXISTS `planning_add_column`;
DROP PROCEDURE IF EXISTS `planning_add_index`;
