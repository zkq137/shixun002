# 梯队规划与预测服务

服务端口为 `8082`，接口前缀为 `/api/planning`。本模块只读取员工、技能、绩效和潜力公共表，写入范围限定在规划模块自己的表。

## 首次初始化

完整数据必须按以下顺序导入。`talent_dataset.sql` 会重建流失预警表，因此规划增量脚本必须最后执行；增量脚本可以重复执行：

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

cmd /c "`"$mysql`" -uroot -p你的密码 --default-character-set=utf8mb4 < sql\talent_db.sql"
cmd /c "`"$mysql`" -uroot -p你的密码 --default-character-set=utf8mb4 < sql\talent_dataset.sql"
cmd /c "`"$mysql`" -uroot -p你的密码 --default-character-set=utf8mb4 < sql\talent_skill_tree.sql"
cmd /c "`"$mysql`" -uroot -p你的密码 --default-character-set=utf8mb4 < sql\talent_analysis.sql"
cmd /c "`"$mysql`" -uroot -p你的密码 --default-character-set=utf8mb4 < talent-planning-service\sql\planning_schema.sql"
```

从旧的 3 人样例库切换到完整数据集时，再执行一次规划快照重置脚本，防止旧岗位和员工 ID 对应的快照混入新数据：

```powershell
cmd /c "`"$mysql`" -uroot -p你的密码 --default-character-set=utf8mb4 < talent-planning-service\sql\reset_planning_snapshots.sql"
```

服务同时兼容早期样例库的 `position_level/work_years` 和完整数据集的 `level_tier/tenure_years`，无需修改公共表结构。

数据库密码也可通过环境变量覆盖：

```powershell
$env:SPRING_DATASOURCE_PASSWORD = "你的本机密码"
```

## 启动

```powershell
$env:JAVA_HOME = "C:\.jdk\microsoft-jdk-17"
& "C:\.maven\maven-3.9.15\bin\mvn.cmd" spring-boot:run -pl talent-planning-service
```

推荐的数据刷新顺序：

1. `POST /api/planning/succession/refresh?positionId={岗位ID}`，逐个刷新关键岗位候选快照。
2. `POST /api/planning/talent-pools/refresh`，根据候选快照汇总人才池。
3. `POST /api/planning/risks/positions/evaluate`，重新计算岗位风险。
4. `GET /api/planning/dashboard`，读取看板汇总。

当前公共样例库没有技能、绩效和潜力样例记录时，候选评分会按缺失维度记 0 分并返回数据缺失提示，这是预期行为。
