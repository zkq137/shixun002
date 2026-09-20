# 梯队规划与预测服务

服务端口为 `8082`，接口前缀为 `/api/planning`。本模块只读取员工、技能、绩效和潜力公共表，写入范围限定在规划模块自己的表。

## 首次初始化

先导入根目录公共脚本，再执行本模块增量脚本。增量脚本可以重复执行：

```powershell
Get-Content -Raw -Encoding UTF8 .\talent-planning-service\sql\planning_schema.sql |
  & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" `
    "--host=127.0.0.1" "--user=root" "--password=20050312" "--default-character-set=utf8mb4"
```

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
