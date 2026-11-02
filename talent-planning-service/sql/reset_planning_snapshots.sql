-- 切换公共数据集后清理规划模块派生快照。
-- 仅清理规划模块自己的候选、人才池和岗位风险，不修改员工、岗位、技能等公共数据。
USE `talent_db`;

DELETE FROM `pos_succession_candidate`;
DELETE FROM `talent_pool`;
DELETE FROM `pos_risk`;
