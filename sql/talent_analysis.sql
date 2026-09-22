-- =====================================================================
-- 数据分析与报告模块（模块五）：建表 + 初始化
-- ---------------------------------------------------------------------
-- 做了三件事：
--   1. model_log  补全模型日志字段（模型名、版本、状态、样本量、耗时、指标、参数、数据版本），
--      用于「模型运行效果分析」和「模型日志溯源」；
--   2. report / report_template 补全自定义报表需要的字段（报表名、维度、指标、过滤条件、
--      明细数据、图表数据、生成时用的模型版本和数据版本）；
--   3. 新建 emp_movement（人才流动记录：入职/晋升/调岗/离职），
--      并用现有员工的司龄倒推出「入职」事件，让流动趋势有历史可看。
--
-- 执行方式：
--   cmd /c "D:\Develop\MySQL80\bin\mysql.exe -uroot -p20050312 --default-character-set=utf8mb4 < sql\talent_analysis.sql"
--
-- 可重复执行：字段存在就跳过，初始数据按唯一条件去重。
-- =====================================================================

USE `talent_db`;
SET NAMES utf8mb4;

-- ----------------------------
-- 1. model_log 补字段
-- ----------------------------
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'model_log' AND column_name = 'model_name');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE model_log ADD COLUMN model_name VARCHAR(64) DEFAULT NULL COMMENT ''模型名称'' AFTER id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'model_log' AND column_name = 'model_type');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE model_log ADD COLUMN model_type VARCHAR(32) DEFAULT NULL COMMENT ''模型类型：分类/排序/回归''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'model_log' AND column_name = 'status');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE model_log ADD COLUMN status VARCHAR(16) DEFAULT ''已完成'' COMMENT ''状态：已完成/失败/训练中''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'model_log' AND column_name = 'sample_count');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE model_log ADD COLUMN sample_count INT DEFAULT 0 COMMENT ''训练样本量''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'model_log' AND column_name = 'train_duration_ms');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE model_log ADD COLUMN train_duration_ms INT DEFAULT 0 COMMENT ''训练耗时(毫秒)''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'model_log' AND column_name = 'metrics');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE model_log ADD COLUMN metrics TEXT COMMENT ''评估指标(JSON)：auc/precision/recall 等''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'model_log' AND column_name = 'dataset_version');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE model_log ADD COLUMN dataset_version VARCHAR(32) DEFAULT NULL COMMENT ''训练用的数据版本''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'model_log' AND column_name = 'remark');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE model_log ADD COLUMN remark VARCHAR(255) DEFAULT NULL COMMENT ''备注''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 2. report / report_template 补字段
-- ----------------------------
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'report' AND column_name = 'report_name');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE report ADD COLUMN report_name VARCHAR(128) DEFAULT NULL COMMENT ''报表名称'' AFTER template_id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'report' AND column_name = 'dimensions');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE report ADD COLUMN dimensions VARCHAR(255) DEFAULT NULL COMMENT ''维度key，逗号分隔''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'report' AND column_name = 'metrics');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE report ADD COLUMN metrics VARCHAR(255) DEFAULT NULL COMMENT ''指标key，逗号分隔''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'report' AND column_name = 'filters');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE report ADD COLUMN filters VARCHAR(500) DEFAULT NULL COMMENT ''过滤条件(JSON)''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'report' AND column_name = 'chart_data');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE report ADD COLUMN chart_data TEXT COMMENT ''图表数据(JSON)''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'report' AND column_name = 'row_count');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE report ADD COLUMN row_count INT DEFAULT 0 COMMENT ''明细行数''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'report' AND column_name = 'model_version');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE report ADD COLUMN model_version VARCHAR(32) DEFAULT NULL COMMENT ''生成时用的模型版本（溯源用）''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'report' AND column_name = 'data_version');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE report ADD COLUMN data_version VARCHAR(32) DEFAULT NULL COMMENT ''生成时的数据快照版本（溯源用）''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'report' AND column_name = 'generated_by');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE report ADD COLUMN generated_by VARCHAR(64) DEFAULT ''当前用户'' COMMENT ''生成人''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 3. 人才流动记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `emp_movement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `employee_id` BIGINT NOT NULL COMMENT '员工ID',
  `emp_no` VARCHAR(32) DEFAULT NULL COMMENT '工号（冗余，方便直接看）',
  `movement_type` VARCHAR(16) NOT NULL COMMENT '类型：入职/晋升/调岗/离职',
  `from_value` VARCHAR(64) DEFAULT NULL COMMENT '变化前（岗位或职级）',
  `to_value` VARCHAR(64) DEFAULT NULL COMMENT '变化后（岗位或职级）',
  `effect_date` DATE NOT NULL COMMENT '生效日期',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_emp` (`employee_id`),
  KEY `idx_date` (`effect_date`),
  KEY `idx_type` (`movement_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人才流动记录';

-- ----------------------------
-- 4. 初始数据
-- ----------------------------

-- 4.1 模型训练日志（演示用：同一模型多个版本 + 一次失败的训练）
INSERT INTO `model_log`
  (`model_name`, `model_type`, `version`, `train_time`, `status`, `sample_count`,
   `train_duration_ms`, `metrics`, `params`, `dataset_version`, `remark`)
SELECT * FROM (
  SELECT '人才流失风险预测' AS a, '分类模型' AS b, 'v1.0' AS c, '2026-03-15 10:20:00' AS d, '已完成' AS e, 800 AS f, 12500 AS g,
         '{"auc":0.78,"precision":0.71,"recall":0.66,"f1":0.68}' AS h,
         '{"algorithm":"LogisticRegression","features":["司龄","绩效评分","薪酬系数","潜力评分"],"testRatio":0.2}' AS i,
         'dataset-v1' AS j, '首个基线版本，只用了四个基础特征' AS k
  UNION ALL SELECT '人才流失风险预测','分类模型','v1.1','2026-05-08 14:05:00','已完成',1000,15800,
         '{"auc":0.83,"precision":0.76,"recall":0.72,"f1":0.74}',
         '{"algorithm":"RandomForest","features":["司龄","绩效评分","薪酬系数","潜力评分","部门","办公方式"],"nEstimators":200}',
         'dataset-v2','加入部门和办公方式特征'
  UNION ALL SELECT '人才流失风险预测','分类模型','v1.2','2026-07-20 09:40:00','已完成',1000,22100,
         '{"auc":0.87,"precision":0.81,"recall":0.78,"f1":0.79}',
         '{"algorithm":"XGBoost","features":["司龄","绩效评分","薪酬系数","潜力评分","部门","办公方式","技能覆盖度"],"maxDepth":6,"lr":0.05}',
         'dataset-v2','调参 + 特征筛选，效果最好的一版'
  UNION ALL SELECT '人才流失风险预测','分类模型','v1.3','2026-09-10 15:50:00','失败',0,3200,
         '{}',
         '{"algorithm":"XGBoost","features":["司龄","绩效评分","薪酬系数","潜力评分","部门","办公方式","技能覆盖度","近三月培训次数"],"maxDepth":8}',
         'dataset-v3','数据集缺少培训次数字段，训练中断，待补齐'
  UNION ALL SELECT '高潜人才识别','分类模型','v1.0','2026-06-11 16:30:00','已完成',1000,18400,
         '{"auc":0.85,"precision":0.79,"recall":0.74,"f1":0.76}',
         '{"algorithm":"LightGBM","features":["绩效评分","潜力评分","司龄","技能数","培训完成度"],"maxDepth":5}',
         'dataset-v2','按 S/A 级潜力标签做二分类'
  UNION ALL SELECT '岗位匹配度推荐','排序模型','v1.0','2026-08-02 11:15:00','已完成',1437,26300,
         '{"ndcg@10":0.72,"precision@10":0.64,"recall@10":0.58}',
         '{"algorithm":"LambdaMART","features":["技能匹配数","岗位核心技能数","绩效评分","司龄"],"k":10}',
         'dataset-v2','给继任候选排序用'
) t
WHERE NOT EXISTS (SELECT 1 FROM model_log m WHERE m.model_name = t.a AND m.version = t.c);

-- 4.2 入职事件：用司龄倒推出入职日期（这是唯一能从现有数据真实推出来的流动事件）
INSERT INTO `emp_movement` (`employee_id`, `emp_no`, `movement_type`, `to_value`, `effect_date`, `remark`)
SELECT e.id, e.emp_no, '入职', p.position_name,
       DATE_SUB(CURDATE(), INTERVAL ROUND(IFNULL(e.tenure_years, 0) * 12) MONTH),
       '根据司龄推算的入职时间'
FROM emp_employee e
LEFT JOIN position p ON p.id = e.position_id
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT employee_id, movement_type FROM emp_movement) m
    WHERE m.employee_id = e.id AND m.movement_type = '入职'
);

-- 4.3 预置几个报表模板
INSERT INTO `report_template` (`template_name`, `dimensions`, `content`)
SELECT * FROM (
  SELECT '人才结构分析' AS a, 'department' AS b,
    '{"dimensions":["department"],"metrics":["count","avgAge","avgTenure","avgSalary"],"filters":{},"description":"看各部门的人数、年龄、司龄和薪酬水平"}' AS c
  UNION ALL SELECT '流失风险分析','department',
    '{"dimensions":["department","warningLevel"],"metrics":["count","highRiskCount","avgTenure","avgPerf"],"filters":{},"description":"按部门和高风险等级看人员分布"}'
  UNION ALL SELECT '高潜人才盘点','department',
    '{"dimensions":["department","potentialLevel"],"metrics":["count","avgPerf","avgPotential","coreTalentCount"],"filters":{"status":"在职"},"description":"各部门高潜人才储备情况"}'
) t
WHERE NOT EXISTS (SELECT 1 FROM report_template r WHERE r.template_name = t.a);

-- ----------------------------
-- 5. 核对
-- ----------------------------
SELECT (SELECT COUNT(*) FROM model_log) AS 模型日志条数,
       (SELECT COUNT(*) FROM emp_movement) AS 流动记录条数,
       (SELECT COUNT(*) FROM emp_movement WHERE movement_type = '入职') AS 入职事件,
       (SELECT COUNT(*) FROM report_template) AS 报表模板数;
