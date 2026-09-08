-- =====================================================================
-- 智能企业人才梯队建设平台 - 共享数据库初始化脚本
-- 执行方式：mysql -uroot -p < talent_db.sql
-- 或直接用 Navicat 等方式运行
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `talent_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `talent_db`;

-- ----------------------------
-- 模块一：员工档案智能管理
-- ----------------------------

-- 员工基础档案
CREATE TABLE IF NOT EXISTS `emp_employee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `emp_no` VARCHAR(32) DEFAULT NULL COMMENT '工号',
  `name` VARCHAR(64) NOT NULL COMMENT '姓名',
  `department` VARCHAR(64) DEFAULT NULL COMMENT '部门',
  `position` VARCHAR(64) DEFAULT NULL COMMENT '岗位',
  `hire_date` DATE DEFAULT NULL COMMENT '入职时间',
  `education` VARCHAR(32) DEFAULT NULL COMMENT '学历',
  `work_years` INT DEFAULT 0 COMMENT '工龄',
  `phone` VARCHAR(32) DEFAULT NULL COMMENT '联系方式',
  `status` VARCHAR(16) DEFAULT '在职' COMMENT '状态: 在职/离职/试用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工基础档案';

-- 技能树
CREATE TABLE IF NOT EXISTS `skill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `skill_name` VARCHAR(64) NOT NULL COMMENT '技能名称',
  `skill_category` VARCHAR(64) DEFAULT NULL COMMENT '技能分类',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父技能ID(0为顶级)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能体系';

-- 员工技能
CREATE TABLE IF NOT EXISTS `emp_skill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT NOT NULL COMMENT '员工ID',
  `skill_id` BIGINT NOT NULL COMMENT '技能ID',
  `skill_grade` VARCHAR(16) DEFAULT NULL COMMENT '技能等级',
  `master_hours` INT DEFAULT 0 COMMENT '掌握时长(小时)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_emp` (`employee_id`),
  KEY `idx_skill` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工技能';

-- 历年生绩效数据
CREATE TABLE IF NOT EXISTS `emp_performance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT NOT NULL COMMENT '员工ID',
  `perf_year` INT NOT NULL COMMENT '绩效年份',
  `score` DECIMAL(5,2) DEFAULT NULL COMMENT '绩效得分',
  `grade` VARCHAR(16) DEFAULT NULL COMMENT '绩效评级',
  `dimension_scores` TEXT DEFAULT NULL COMMENT '考核维度得分(JSON)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_emp` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历年生绩效';

-- 潜力评估
CREATE TABLE IF NOT EXISTS `emp_potential` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT NOT NULL COMMENT '员工ID',
  `indicator_values` TEXT DEFAULT NULL COMMENT '潜力评估指标值(JSON)',
  `total_score` DECIMAL(5,2) DEFAULT NULL COMMENT '潜力综合得分',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_emp` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工潜力评估';

-- ----------------------------
-- 模块二：梯队规划与预测
-- ----------------------------

-- 岗位
CREATE TABLE IF NOT EXISTS `position` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `position_name` VARCHAR(64) NOT NULL COMMENT '岗位名称',
  `position_level` VARCHAR(32) DEFAULT NULL COMMENT '岗位层级',
  `is_key` TINYINT DEFAULT 0 COMMENT '关键岗位标识 0否 1是',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位';

-- 关键岗位继任候选人
CREATE TABLE IF NOT EXISTS `pos_succession_candidate` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `position_id` BIGINT NOT NULL COMMENT '岗位ID',
  `employee_id` BIGINT NOT NULL COMMENT '候选人ID',
  `match_score` DECIMAL(5,2) DEFAULT NULL COMMENT '匹配得分',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pos` (`position_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='继任候选人';

-- 岗位风险
CREATE TABLE IF NOT EXISTS `pos_risk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `position_id` BIGINT NOT NULL COMMENT '岗位ID',
  `risk_level` VARCHAR(16) DEFAULT NULL COMMENT '风险等级',
  `risk_desc` VARCHAR(255) DEFAULT NULL COMMENT '风险描述',
  `risk_scope` VARCHAR(255) DEFAULT NULL COMMENT '风险影响范围',
  `checked_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pos` (`position_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位风险评估';

-- 人才池容量
CREATE TABLE IF NOT EXISTS `talent_pool` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `level` VARCHAR(32) DEFAULT NULL COMMENT '层级',
  `total_capacity` INT DEFAULT 0 COMMENT '总容量',
  `current_count` INT DEFAULT 0 COMMENT '现有人才数量',
  `gap_count` INT DEFAULT 0 COMMENT '缺口数量',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人才池容量';

-- 流失预警规则
CREATE TABLE IF NOT EXISTS `resign_warning_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_name` VARCHAR(64) DEFAULT NULL COMMENT '规则名称',
  `trigger_condition` VARCHAR(255) DEFAULT NULL COMMENT '触发条件',
  `warning_level` VARCHAR(16) DEFAULT NULL COMMENT '预警等级',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流失预警规则';

-- 流失预警记录
CREATE TABLE IF NOT EXISTS `resign_warning_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT NOT NULL COMMENT '员工ID',
  `risk_score` DECIMAL(5,2) DEFAULT NULL COMMENT '流失风险分值',
  `warning_level` VARCHAR(16) DEFAULT NULL COMMENT '预警等级',
  `warning_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '预警时间',
  `handle_status` VARCHAR(16) DEFAULT '未处理' COMMENT '处理状态',
  PRIMARY KEY (`id`),
  KEY `idx_emp` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流失预警记录';

-- ----------------------------
-- 模块三：智能培训推荐
-- ----------------------------

-- 培训课程
CREATE TABLE IF NOT EXISTS `course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_name` VARCHAR(128) NOT NULL COMMENT '课程名称',
  `course_type` VARCHAR(32) DEFAULT NULL COMMENT '课程类型',
  `for_position` VARCHAR(64) DEFAULT NULL COMMENT '适用岗位',
  `difficulty` VARCHAR(16) DEFAULT NULL COMMENT '难度等级',
  `duration` INT DEFAULT 0 COMMENT '培训时长(小时)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训课程';

-- 学习路径模板
CREATE TABLE IF NOT EXISTS `learning_path` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `path_name` VARCHAR(64) DEFAULT NULL COMMENT '路径名称',
  `target_skill` VARCHAR(64) DEFAULT NULL COMMENT '路径适配技能',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习路径模板';

-- 阶段学习任务
CREATE TABLE IF NOT EXISTS `path_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `path_id` BIGINT NOT NULL COMMENT '学习路径ID',
  `task_name` VARCHAR(128) DEFAULT NULL COMMENT '任务名称',
  `stage` INT DEFAULT 1 COMMENT '阶段',
  `course_id` BIGINT DEFAULT NULL COMMENT '关联课程',
  PRIMARY KEY (`id`),
  KEY `idx_path` (`path_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路径任务';

-- 培训参与记录
CREATE TABLE IF NOT EXISTS `training_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT NOT NULL COMMENT '员工ID',
  `course_id` BIGINT DEFAULT NULL COMMENT '课程ID',
  `attend_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '培训参与率',
  `pass_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '考核通过率',
  `improve_score` DECIMAL(5,2) DEFAULT NULL COMMENT '能力提升分值',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_emp` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培训记录';

-- ----------------------------
-- 模块四：晋升决策支持
-- ----------------------------

-- 晋升候选人
CREATE TABLE IF NOT EXISTS `promo_candidate` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `target_position_id` BIGINT NOT NULL COMMENT '晋升岗位ID',
  `employee_id` BIGINT NOT NULL COMMENT '候选人ID',
  `final_score` DECIMAL(5,2) DEFAULT NULL COMMENT '综合得分',
  `promo_adapt_score` DECIMAL(5,2) DEFAULT NULL COMMENT '晋升适配度分值',
  `position_match_score` DECIMAL(5,2) DEFAULT NULL COMMENT '岗位匹配度',
  `risk_tip` VARCHAR(255) DEFAULT NULL COMMENT '风险隐患',
  `conclusion` VARCHAR(255) DEFAULT NULL COMMENT '评估结论',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='晋升候选人';

-- 历史晋升案例
CREATE TABLE IF NOT EXISTS `promo_case` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `case_name` VARCHAR(64) DEFAULT NULL COMMENT '案例名称',
  `scenario` VARCHAR(255) DEFAULT NULL COMMENT '适用场景',
  `content` TEXT DEFAULT NULL COMMENT '案例内容',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='晋升历史案例';

-- 晋升评估报告
CREATE TABLE IF NOT EXISTS `promo_assessment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `candidate_id` BIGINT DEFAULT NULL COMMENT '候选人ID',
  `dimension_scores` TEXT DEFAULT NULL COMMENT '各维度对比分值(JSON)',
  `report_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报告生成时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='晋升评估报告';

-- ----------------------------
-- 模块五：数据分析与报告
-- ----------------------------

-- 报表模板
CREATE TABLE IF NOT EXISTS `report_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_name` VARCHAR(64) DEFAULT NULL COMMENT '模板名称',
  `dimensions` VARCHAR(255) DEFAULT NULL COMMENT '报表维度',
  `content` TEXT DEFAULT NULL COMMENT '模板定义(JSON)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表模板';

-- 报表
CREATE TABLE IF NOT EXISTS `report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT DEFAULT NULL COMMENT '模板ID',
  `report_data` TEXT DEFAULT NULL COMMENT '数据(JSON)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表';

-- 模型日志
CREATE TABLE IF NOT EXISTS `model_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `train_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '训练时间',
  `params` TEXT DEFAULT NULL COMMENT '训练参数(JSON)',
  `accuracy` DECIMAL(5,2) DEFAULT NULL COMMENT '准确率',
  `version` VARCHAR(32) DEFAULT NULL COMMENT '迭代版本',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型训练日志';

-- ----------------------------
-- 示例数据（少量，便于联调）
-- ----------------------------
INSERT INTO `emp_employee` (`emp_no`, `name`, `department`, `position`, `education`, `work_years`, `status`)
VALUES
  ('E001', '张三', '研发部', 'Java开发', '本科', 5, '在职'),
  ('E002', '李四', '产品部', '产品经理', '硕士', 3, '在职'),
  ('E003', '王五', '研发部', '架构师', '本科', 8, '在职');

INSERT INTO `position` (`position_name`, `position_level`, `is_key`)
VALUES
  ('技术经理', '管理岗', 1),
  ('Java开发', '核心岗', 1),
  ('产品经理', '核心岗', 0);
