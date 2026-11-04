-- =====================================================================
-- 员工技能关系补齐：修复“岗位要求存在，但全公司具备人数为 0”的数据异常
-- 适用：talent_dataset.sql 导入后的演示库；脚本可重复执行。
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 < sql/talent_skill_employee_fix.sql
-- =====================================================================
USE `talent_db`;

-- 找出岗位要求中存在、但全公司没有任何员工具备的技能。
-- 对当前岗位实际要求这些技能的在职员工，按稳定规则抽取约 1/6 补充技能关系，
-- 避免把岗位的全部技能要求都直接复制给员工。
INSERT IGNORE INTO `emp_skill` (`employee_id`, `skill_id`)
SELECT e.id, req.skill_id
FROM `emp_employee` e
JOIN `pos_skill_require` req ON req.position_id = e.position_id
JOIN (
    SELECT r.skill_id
    FROM `pos_skill_require` r
    LEFT JOIN `emp_skill` es ON es.skill_id = r.skill_id
    WHERE es.id IS NULL
    GROUP BY r.skill_id
) uncovered ON uncovered.skill_id = req.skill_id
LEFT JOIN `emp_skill` own_skill
       ON own_skill.employee_id = e.id AND own_skill.skill_id = req.skill_id
WHERE e.status = '在职'
  AND own_skill.id IS NULL
  AND MOD(e.id * 17 + req.skill_id * 13, 6) = 0
ORDER BY e.id, req.skill_id;

-- 稀有技能可能没有被 1/6 抽样命中，至少为每个仍无人具备的岗位要求技能补 1 名在职员工。
INSERT IGNORE INTO `emp_skill` (`employee_id`, `skill_id`)
SELECT seed.employee_id, seed.skill_id
FROM (
    SELECT r.skill_id, MIN(e.id) AS employee_id
    FROM `pos_skill_require` r
    JOIN `emp_employee` e ON e.position_id = r.position_id AND e.status = '在职'
    WHERE NOT EXISTS (
        SELECT 1 FROM `emp_skill` any_skill WHERE any_skill.skill_id = r.skill_id
    )
    GROUP BY r.skill_id
) seed;

SELECT COUNT(*) AS `员工技能关系数`,
       COUNT(DISTINCT skill_id) AS `有员工具备的技能数`
FROM `emp_skill`;

SELECT COUNT(DISTINCT r.skill_id) AS `仍有岗位要求但无人具备的技能数`
FROM `pos_skill_require` r
LEFT JOIN `emp_skill` es ON es.skill_id = r.skill_id
WHERE es.id IS NULL;
