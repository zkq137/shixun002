-- =====================================================================
-- 智能培训演示数据补齐：课程属性与学习效果
-- 适用：2026-09 导入的 1000 人演示数据集；脚本可重复执行。
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 < sql/talent_training_data_fix.sql
-- =====================================================================
USE `talent_db`;

DROP TEMPORARY TABLE IF EXISTS `tmp_course_training_meta`;
CREATE TEMPORARY TABLE `tmp_course_training_meta` (
  `id` BIGINT NOT NULL PRIMARY KEY,
  `course_type` VARCHAR(32) NOT NULL,
  `difficulty` VARCHAR(16) NOT NULL,
  `duration` INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tmp_course_training_meta` (`id`, `course_type`, `difficulty`, `duration`) VALUES
(1,'数据分析','进阶',6),
(2,'管理能力','进阶',8),
(3,'通用素养','入门',4),
(4,'通用素养','入门',3),
(5,'通用素养','进阶',5),
(6,'通用素养','入门',3),
(7,'通用素养','进阶',4),
(8,'技术研发','进阶',6),
(9,'人力资源','进阶',8),
(10,'通用素养','入门',2),
(11,'产品管理','进阶',10),
(12,'业务运营','入门',5),
(13,'人力资源','进阶',6),
(14,'通用素养','入门',4),
(15,'业务运营','进阶',7),
(16,'管理能力','进阶',12),
(17,'通用素养','入门',3),
(18,'人力资源','进阶',5),
(19,'业务运营','进阶',8),
(20,'通用素养','入门',3),
(21,'数字化技能','进阶',6),
(22,'通用素养','入门',3),
(23,'通用素养','进阶',5),
(24,'管理能力','入门',4),
(25,'业务运营','进阶',6),
(26,'数字化技能','进阶',10),
(27,'通用素养','进阶',4),
(28,'通用素养','入门',3),
(29,'业务运营','入门',4),
(30,'产品管理','进阶',7),
(31,'业务运营','入门',4),
(32,'业务运营','进阶',6),
(33,'业务运营','进阶',5),
(34,'业务运营','进阶',6),
(35,'业务运营','进阶',7),
(36,'市场营销','进阶',6),
(37,'财务专业','进阶',8),
(38,'人力资源','进阶',8),
(39,'人力资源','进阶',6),
(40,'业务运营','进阶',6),
(41,'技术研发','入门',8),
(42,'技术研发','进阶',6),
(43,'技术研发','入门',4),
(44,'技术研发','进阶',6),
(45,'技术研发','进阶',10),
(46,'技术研发','进阶',8),
(47,'技术研发','高级',12),
(48,'技术研发','高级',8),
(49,'数字化技能','高级',8),
(50,'数字化技能','进阶',8),
(51,'数字化技能','进阶',6),
(52,'设计能力','入门',4),
(53,'设计能力','入门',5),
(54,'技术研发','入门',5),
(55,'技术研发','进阶',8),
(56,'通用素养','入门',3),
(57,'通用素养','入门',2),
(58,'通用素养','入门',4);

UPDATE `course` c
JOIN `tmp_course_training_meta` t ON t.id = c.id
SET c.course_type = t.course_type,
    c.difficulty = t.difficulty,
    c.duration = t.duration
WHERE c.course_type IS NULL
   OR c.difficulty IS NULL
   OR c.duration IS NULL
   OR c.duration = 0;

UPDATE `course` c
JOIN (
    SELECT req.course_id,
           COUNT(DISTINCT req.position_id) AS position_count,
           GROUP_CONCAT(DISTINCT p.position_name ORDER BY p.position_name SEPARATOR '、') AS position_names
    FROM pos_course_require req
    JOIN position p ON p.id = req.position_id
    GROUP BY req.course_id
) related ON related.course_id = c.id
SET c.for_position = CASE
        WHEN related.position_count <= 4 THEN related.position_names
        ELSE CONCAT('多岗位通用（', related.position_count, ' 个岗位）')
    END
WHERE c.for_position IS NULL
   OR c.for_position = '';

-- 学习路径模板：按“岗位能力成长”组织课程，page 展示的是可复用的阶段模板
DROP TEMPORARY TABLE IF EXISTS `tmp_learning_path_seed`;
CREATE TEMPORARY TABLE `tmp_learning_path_seed` (
  `path_name` VARCHAR(64) NOT NULL,
  `target_skill` VARCHAR(64) NOT NULL,
  `task_name` VARCHAR(128) NOT NULL,
  `stage` INT NOT NULL,
  `course_id` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tmp_learning_path_seed` (`path_name`, `target_skill`, `task_name`, `stage`, `course_id`) VALUES
('新员工职业素养启航路径','沟通协作、职业素养','学习办公与行政规范',1,57),
('新员工职业素养启航路径','沟通协作、职业素养','塑造商务礼仪与职业形象',1,10),
('新员工职业素养启航路径','沟通协作、职业素养','掌握高效沟通方法',2,3),
('新员工职业素养启航路径','沟通协作、职业素养','建立时间管理习惯',2,28),
('新员工职业素养启航路径','沟通协作、职业素养','提升团队协作能力',3,14),
('新员工职业素养启航路径','沟通协作、职业素养','制定个人职业发展规划',3,22),
('新任管理者领导力路径','目标管理、团队领导','建立目标管理框架',1,24),
('新任管理者领导力路径','目标管理、团队领导','提升管理沟通能力',1,3),
('新任管理者领导力路径','目标管理、团队领导','打造高协作团队',2,14),
('新任管理者领导力路径','目标管理、团队领导','落地绩效管理',2,13),
('新任管理者领导力路径','目标管理、团队领导','完成领导力进阶',3,2),
('新任管理者领导力路径','目标管理、团队领导','掌握项目管理方法',3,16),
('技术研发工程能力路径','编码规范、工程化、后端架构','统一编码规范',1,43),
('技术研发工程能力路径','编码规范、工程化、后端架构','建立单元测试能力',1,44),
('技术研发工程能力路径','编码规范、工程化、后端架构','理解敏捷研发流程',2,8),
('技术研发工程能力路径','编码规范、工程化、后端架构','搭建 CI/CD 工程链路',2,42),
('技术研发工程能力路径','编码规范、工程化、后端架构','掌握工程化实践',3,46),
('技术研发工程能力路径','编码规范、工程化、后端架构','进阶后端架构设计',3,47),
('数据分析实战路径','数据分析、BI与数据治理','建立数据分析思维',1,1),
('数据分析实战路径','数据分析、BI与数据治理','掌握 Excel 高级分析',1,21),
('数据分析实战路径','数据分析、BI与数据治理','使用 Python 处理数据',2,26),
('数据分析实战路径','数据分析、BI与数据治理','构建 BI 分析看板',2,51),
('数据分析实战路径','数据分析、BI与数据治理','建设 ETL 数据链路',3,50),
('数据分析实战路径','数据分析、BI与数据治理','完善数据治理体系',3,49),
('销售与客户成功路径','商务谈判、客户经营、销售增长','掌握销售谈判技巧',1,19),
('销售与客户成功路径','商务谈判、客户经营、销售增长','提升商务谈判能力',1,32),
('销售与客户成功路径','商务谈判、客户经营、销售增长','优化客户服务体验',2,29),
('销售与客户成功路径','商务谈判、客户经营、销售增长','强化销售成单技巧',2,40),
('销售与客户成功路径','商务谈判、客户经营、销售增长','建立客户成功体系',3,33),
('销售与客户成功路径','商务谈判、客户经营、销售增长','拓展私域流量运营',3,25),
('产品与体验设计路径','产品规划、用户体验、原型设计','训练结构化思考',1,7),
('产品与体验设计路径','产品规划、用户体验、原型设计','培养创新思维',1,27),
('产品与体验设计路径','产品规划、用户体验、原型设计','掌握用户体验设计',2,30),
('产品与体验设计路径','产品规划、用户体验、原型设计','使用 Figma 完成设计',2,52),
('产品与体验设计路径','产品规划、用户体验、原型设计','进阶产品规划能力',3,11),
('产品与体验设计路径','产品规划、用户体验、原型设计','完成原型工具实战',3,53);

INSERT INTO `learning_path` (`path_name`, `target_skill`)
SELECT seed.path_name, MAX(seed.target_skill)
FROM `tmp_learning_path_seed` seed
WHERE NOT EXISTS (
    SELECT 1 FROM `learning_path` path WHERE path.path_name = seed.path_name
)
GROUP BY seed.path_name;

INSERT INTO `path_task` (`path_id`, `task_name`, `stage`, `course_id`)
SELECT path.id, seed.task_name, seed.stage, seed.course_id
FROM `tmp_learning_path_seed` seed
JOIN `learning_path` path ON path.path_name = seed.path_name
WHERE NOT EXISTS (
    SELECT 1 FROM `path_task` task WHERE task.path_id = path.id
)
ORDER BY path.id, seed.stage, seed.course_id;

DROP TEMPORARY TABLE IF EXISTS `tmp_learning_path_seed`;

UPDATE `training_record`
SET attend_rate = COALESCE(
        attend_rate,
        78 + MOD(id * 7 + employee_id * 3, 23)
    ),
    pass_rate = COALESCE(
        pass_rate,
        66 + MOD(id * 11 + employee_id * 5 + course_id * 7, 35)
    ),
    improve_score = COALESCE(
        improve_score,
        ROUND(4 + MOD(id * 13 + employee_id * 7 + course_id * 11, 161) / 10, 2)
    )
WHERE attend_rate IS NULL
   OR pass_rate IS NULL
   OR improve_score IS NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_course_training_meta`;

SELECT COUNT(*) AS `课程数`,
       SUM(duration > 0) AS `已配置时长课程数`,
       SUM(for_position IS NOT NULL AND for_position <> '') AS `已配置适用岗位课程数`,
       MIN(duration) AS `最短时长`,
       MAX(duration) AS `最长时长`
FROM `course`;

SELECT COUNT(*) AS `学习记录数`,
       SUM(attend_rate IS NOT NULL) AS `已配置参与率`,
       SUM(pass_rate IS NOT NULL) AS `已配置通过率`,
       SUM(improve_score IS NOT NULL) AS `已配置能力提升`
FROM `training_record`;

SELECT COUNT(*) AS `学习路径模板数`,
       (SELECT COUNT(*) FROM `path_task`) AS `路径任务数`
FROM `learning_path`;
