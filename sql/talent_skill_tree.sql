-- =====================================================================
-- 技能树初始化：把 skill 表从「一张平表」变成「分类 + 子技能」两层结构
-- ---------------------------------------------------------------------
-- 做完的事：
--   1. skill 表补上 parent_id / level / sort_order / status / description 五个字段；
--   2. 建 9 个顶级分类（研发技术、数据与算法、产品与设计、运营与营销、销售与客户、
--      财务与风控、人力与组织、行政与流程、通用职业能力）；
--   3. 把数据集里的 172 个技能挂到对应分类下（同时回填 skill_category）。
--
-- 执行方式：
--   cmd /c "D:\Develop\MySQL80\bin\mysql.exe -uroot -p20050312 --default-character-set=utf8mb4 < sql\talent_skill_tree.sql"
--
-- 本脚本可以重复执行：字段已存在就跳过，分类按名字去重，技能挂载是覆盖写。
-- 自己新建的技能不会被脚本改动。
-- =====================================================================

USE `talent_db`;
SET NAMES utf8mb4;

-- ----------------------------
-- 1. 补字段（存在就跳过）
-- ----------------------------
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'skill' AND column_name = 'parent_id');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE skill ADD COLUMN parent_id BIGINT NOT NULL DEFAULT 0 COMMENT ''父技能ID，0=顶级分类'' AFTER skill_name', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'skill' AND column_name = 'level');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE skill ADD COLUMN level TINYINT NOT NULL DEFAULT 1 COMMENT ''层级：1=分类 2=技能''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'skill' AND column_name = 'sort_order');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE skill ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT ''同级排序''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'skill' AND column_name = 'status');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE skill ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT ''启用'' COMMENT ''状态：启用/停用''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'skill' AND column_name = 'description');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE skill ADD COLUMN description VARCHAR(255) DEFAULT NULL COMMENT ''技能说明''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'skill' AND index_name = 'idx_skill_parent');
SET @ddl := IF(@has_idx = 0, 'ALTER TABLE skill ADD INDEX idx_skill_parent (parent_id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 2. 顶级分类（按名字去重）
-- ----------------------------
INSERT INTO `skill` (`skill_name`, `parent_id`, `level`, `sort_order`, `skill_category`, `status`) VALUES
('研发技术', 0, 1, 1, '研发技术', '启用'),
('数据与算法', 0, 1, 2, '数据与算法', '启用'),
('产品与设计', 0, 1, 3, '产品与设计', '启用'),
('运营与营销', 0, 1, 4, '运营与营销', '启用'),
('销售与客户', 0, 1, 5, '销售与客户', '启用'),
('财务与风控', 0, 1, 6, '财务与风控', '启用'),
('人力与组织', 0, 1, 7, '人力与组织', '启用'),
('行政与流程', 0, 1, 8, '行政与流程', '启用'),
('通用职业能力', 0, 1, 9, '通用职业能力', '启用')
ON DUPLICATE KEY UPDATE `parent_id` = 0, `level` = 1, `sort_order` = VALUES(`sort_order`), `status` = '启用';

-- ----------------------------
-- 3. 把技能挂到分类下
-- ----------------------------
-- 研发技术
SET @cat_id := (SELECT id FROM skill WHERE skill_name = '研发技术' AND parent_id = 0 LIMIT 1);
UPDATE skill SET parent_id = @cat_id, level = 2, skill_category = '研发技术'
WHERE skill_name IN ('单元测试', 'Go', 'Linux', 'Docker', 'React', 'Spring Boot', 'Git', '微服务', 'Java', 'K8s', 'CI/CD', 'Redis', 'MySQL', 'Python', '敏捷开发', 'SQL', 'Vue', '接口测试', '性能优化', '数据结构', '技术架构能力', '架构理解', '敏捷开发Scrum');

-- 数据与算法
SET @cat_id := (SELECT id FROM skill WHERE skill_name = '数据与算法' AND parent_id = 0 LIMIT 1);
UPDATE skill SET parent_id = @cat_id, level = 2, skill_category = '数据与算法'
WHERE skill_name IN ('数据分析', '算法', '深度学习', 'Hadoop', '模型调优', '数据挖掘', 'Spark', 'PyTorch', 'TensorFlow', 'Tableau', '数据可视化', '统计学', 'Hive', '特征工程', '机器学习', '数据建模', 'Python数据分析', '数据分析思维', '数据分析能力', '可视化表达', '分析能力');

-- 产品与设计
SET @cat_id := (SELECT id FROM skill WHERE skill_name = '产品与设计' AND parent_id = 0 LIMIT 1);
UPDATE skill SET parent_id = @cat_id, level = 2, skill_category = '产品与设计'
WHERE skill_name IN ('需求分析', 'Sketch', 'Figma', '用户研究', '交互设计', 'Axure', '需求挖掘', '竞品分析', 'UI设计', '用户体验', '原型设计', '产品设计', 'PRD撰写', '用户体验设计', '用户体验理解', '产品经理进阶', 'A/B测试');

-- 运营与营销
SET @cat_id := (SELECT id FROM skill WHERE skill_name = '运营与营销' AND parent_id = 0 LIMIT 1);
UPDATE skill SET parent_id = @cat_id, level = 2, skill_category = '运营与营销'
WHERE skill_name IN ('运营管理', '用户增长', '渠道拓展', '私域运营', '社交媒体', '短视频运营', '社群运营', '活动策划', '直播运营', '市场策划', 'KOL合作', '内容营销', '品牌推广', 'SEO', 'SEM', '文案撰写', '私域流量运营', '渠道能力', '增长黑客', '业绩目标');

-- 销售与客户
SET @cat_id := (SELECT id FROM skill WHERE skill_name = '销售与客户' AND parent_id = 0 LIMIT 1);
UPDATE skill SET parent_id = @cat_id, level = 2, skill_category = '销售与客户'
WHERE skill_name IN ('投诉处理', '客户成功', '售后服务', '大客户管理', '客户管理', '关系维护', '商务谈判', '销售技巧', '回款管理', '招投标', 'CRM', '客户服务', '客户服务技巧', '销售谈判技巧');

-- 财务与风控
SET @cat_id := (SELECT id FROM skill WHERE skill_name = '财务与风控' AND parent_id = 0 LIMIT 1);
UPDATE skill SET parent_id = @cat_id, level = 2, skill_category = '财务与风控'
WHERE skill_name IN ('供应链', '财务建模', 'ERP系统', '投融资', '会计核算', '税务筹划', '风险管理', '财务报表', '预算管理', '成本控制', '资金管理', '合规管理', '内控审计', '财务分析', '库存管理', '供应商管理');

-- 人力与组织
SET @cat_id := (SELECT id FROM skill WHERE skill_name = '人力与组织' AND parent_id = 0 LIMIT 1);
UPDATE skill SET parent_id = @cat_id, level = 2, skill_category = '人力与组织'
WHERE skill_name IN ('HRBP', '组织发展', '招聘配置', '面试技巧', 'LD', 'TD', '培训发展', '人才测评', '员工关系', '薪酬福利', 'OD', '绩效管理', '人才盘点', '企业文化', '招聘面试技巧', '薪酬设计', '职业规划', '领导力提升');

-- 行政与流程
SET @cat_id := (SELECT id FROM skill WHERE skill_name = '行政与流程' AND parent_id = 0 LIMIT 1);
UPDATE skill SET parent_id = @cat_id, level = 2, skill_category = '行政与流程'
WHERE skill_name IN ('行政管理', '前台接待', '会议组织', '办公用品', '劳动法', '差旅管理', '档案管理', '制度建设', '后勤保障', '固定资产', 'SOP制定', '流程优化');

-- 通用职业能力
SET @cat_id := (SELECT id FROM skill WHERE skill_name = '通用职业能力' AND parent_id = 0 LIMIT 1);
UPDATE skill SET parent_id = @cat_id, level = 2, skill_category = '通用职业能力'
WHERE skill_name IN ('跨部门协作', '团队管理', '项目管理', '目标管理', '效率提升', '公文写作', '高效沟通技巧', '结构化思考', '协同推进', '业务理解', '商务礼仪', '团队协作', '压力管理', '问题解决', '创新思维', '演讲与表达', '时间管理', '协作能力', '质量意识', '表达能力', '沟通协作', '执行力', 'PPT商务演示', '情绪管理', 'Excel高级', 'Excel高级应用', '项目协同', '协同能力', '项目管理PMP', '质量管理', '方案撰写');

-- ----------------------------
-- 4. 核对结果
-- ----------------------------
SELECT p.`skill_name` AS 分类, COUNT(c.id) AS 子技能数
FROM `skill` p LEFT JOIN `skill` c ON c.`parent_id` = p.id
WHERE p.`parent_id` = 0
GROUP BY p.id, p.`skill_name`, p.`sort_order`
ORDER BY p.`sort_order`;

SELECT COUNT(*) AS 未归类技能数 FROM `skill` WHERE `level` = 2 AND `parent_id` = 0;
