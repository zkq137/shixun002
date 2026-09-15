# 智能企业人才梯队建设平台（微服务骨架）

基于 **Spring Boot 3.2 + Spring Cloud 2023 + Spring Cloud Alibaba + Nacos** 的多模块微服务工程，5 人各负责一个业务模块。

> **想直接跑起来？看《[启动运行指南.md](启动运行指南.md)》**——里面有环境要求、一键启动脚本、
> 验证清单和常见问题。数据已经灌好（1000 名员工 + 72 个岗位），五个模块都是查真库，不再是写死的示例数据。
>
> 前端在 `talent-web`（Vue 3 + Vite + TypeScript + Element Plus），启动方式见
> [talent-web/README.md](talent-web/README.md)。

## 一、工程结构

```
talent-platform（父工程）
├─ talent-common                 公共模块：Result / 异常 / 分页
├─ talent-gateway                网关：统一入口 9090
├─ talent-employee-service       模块一 员工档案智能管理   8081
├─ talent-planning-service       模块二 梯队规划与预测     8082
├─ talent-training-service       模块三 智能培训推荐       8083
├─ talent-promotion-service      模块四 晋升决策支持       8084
└─ talent-analysis-service       模块五 数据分析与报告     8085
sql/
   talent_db.sql                 共享库建表脚本
```

## 二、分工建议

| 成员 | 负责模块 | 目录 |
|---|---|---|
| 1 | 员工档案 | talent-employee-service |
| 2 | 梯队规划 | talent-planning-service |
| 3 | 智能培训 | talent-training-service |
| 4 | 晋升决策 | talent-promotion-service |
| 5 | 数据分析 | talent-analysis-service |

每人只在自己模块目录下开发，各开一个 Git feature 分支，最大限度避免冲突。网关和公共模块可指定一人或轮流维护。

## 三、前置环境

- JDK **17**（IDEA 中 Project SDK / Maven 编译用 JDK 17）
- Maven 3.6+
- MySQL 8（执行 `sql/talent_db.sql`）
- **Nacos Server 2.4.x**（若用 JDK 8 启动 Nacos 2.4；服务本身用 JDK 17）

> Nacos 2.4.0.1 建议用 JDK 8 启动；微服务用 JDK 17 编译运行。二者不冲突。

## 四、运行步骤

1. 启动 Nacos（单机即可）：
   ```bat
   startup.cmd -m standalone
   ```
   访问 http://127.0.0.1:8848/nacos ，账号 `nacos / nacos`。

2. 初始化数据库：
   ```bat
   mysql -uroot -p < sql\talent_db.sql
   ```

3. 根目录执行 `mvn clean install`（首次会下载依赖，需要联网）。

4. 依次启动（IDEA 里分别 Run 各 Application 即可）：
   - `GatewayApplication` (9090)
   - `EmployeeServiceApplication` (8081)
   - `PlanningServiceApplication` (8082)
   - `TrainingServiceApplication` (8083)
   - `PromotionServiceApplication` (8084)
   - `AnalysisServiceApplication` (8085)

5. 访问网关：`http://127.0.0.1:9090/api/employee/list`

## 五、Nacos 配置中心（可选但推荐）

每个服务的 `application.yml` 已通过 `spring.config.import` 声明从 Nacos 拉取
`${application-name}-dev.yaml`。在 Nacos 控制台配置管理里新建对应 DataId，即可集中管理配置、免重启热更新。

例如新建 `talent-employee-service-dev.yaml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/talent_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 20050312
```

## 六、队列/端口说明

- 网关：9090
- 服务：8081~8085
- Nacos：8848（HTTP）、9848/9849（gRPC）、7848（Raft，集群需要）
- Sentinel：控制台 8858，各服务与控制台通信的端口 8720~8725（详见第八节）

## 七、接数据库说明

五个业务模块的 `application.yml` 里数据库连接**已经配好了**（talent_db / root / 20050312），
换自己的账号密码时改这一处即可。第一次跑之前先确认库已建好：

1. 先执行 `sql/talent_db.sql`，建出骨架的表结构。
2. 再执行 `sql/talent_dataset.sql`，灌入 1000 名员工 + 72 个岗位的真实数据（见第九节）。
3. 数据库连接已经在 `application.yml` 里配好，按需改成自己的账号密码。
4. 在 `com.talent.<module>.mapper` 下添加 MyBatis-Plus Mapper 接口，`entity` 包下加实体。

## 八、Sentinel 限流与熔断

已接入 **Sentinel 1.8.8**。版本由父工程里的 spring-cloud-alibaba BOM 统一管理，
模块 `pom.xml` 里不用写版本号。

### 1. 端口分配

| 用途 | 端口 |
|---|---|
| Sentinel 控制台（Web） | 8858 |
| 控制台自己占用的传输端口 | 8719 |
| talent-gateway | 8724 |
| talent-employee-service | 8720 |
| talent-planning-service | 8721 |
| talent-training-service | 8722 |
| talent-promotion-service | 8723 |
| talent-analysis-service | 8725 |

> 控制台会占用 Sentinel 默认的 8719，所以各服务从 8720 开始编号，别再改回 8719。

### 2. 启动控制台

控制台是个独立的 jar（本机放在 `D:\Shixun\sentinel-dashboard-1.8.8.jar`，
也可以从 https://github.com/alibaba/Sentinel/releases/tag/1.8.8 下载）：

```bat
java -Dserver.port=8858 -Dsentinel.dashboard.auth.username=sentinel -Dsentinel.dashboard.auth.password=sentinel -jar D:\Shixun\sentinel-dashboard-1.8.8.jar
```

浏览器打开 http://127.0.0.1:8858 ，账号密码都是 `sentinel`。
控制台不启动也不影响业务，只是看不到监控、配不了规则。

### 3. 怎么确认接好了

1. 依次启动 Nacos、Sentinel 控制台、网关、员工服务。
2. 在 cmd 里连续请求 6 次员工列表（网关规则是每秒 3 次）：

```bat
for /l %i in (1,1,6) do @curl -s -o nul -w "%{http_code}\n" http://127.0.0.1:9090/api/employee/list
```

3. 前 3 次返回 `200`，第 4 次开始返回 `429` 和 `{"code":429,"message":"请求过于频繁，请稍后再试"}`，
   说明网关限流已生效；等 1 秒后再请求又能通过。
4. 控制台左侧「机器列表」里能看到 `talent-gateway`、`talent-employee-service` 等，
   IP 是 `127.0.0.1`、状态是健康的，说明客户端已注册成功。

### 4. 内置的示例

| 位置 | 内容 |
|---|---|
| `talent-gateway` 的 `SentinelGatewayRuleConfig` | 两条网关规则：`employee_api` 分组（`/api/employee/**` 每秒 3 次）、`talent-training` 路由每秒 5 次 |
| `EmployeeController.list` | `@SentinelResource("employeeList")` + 兜底方法 `listBlockHandler`，被限流时返回 429 而不是抛异常 |
| `EmployeeClient` + `EmployeeClientFallbackFactory` | 员工服务调不通或被熔断时走降级，返回 503 和原因 |

### 5. 在控制台里加规则

- **接口限流**：控制台 → 簇点链路 → 找到资源名（`employeeList` 或 `/api/employee/list`）→ 点「流控」。
  `@SentinelResource` 里的名字和接口路径都会各自成为一个资源，想控哪个就选哪个。
- **熔断降级**：Feign 调用的资源名形如 `GET:http://talent-employee-service/api/employee/{id}`，
  在「簇点链路」里找到它，点「熔断」即可。
- 控制台里配的规则默认存在内存，**重启就没了**。要持久化得把规则放到 Nacos
  （配 `spring.cloud.sentinel.datasource`，依赖已经在 starter 里）。

### 6. 本机踩过的两个坑（已在配置里规避）

1. **注册上去的 IP 不对**：这台机器装了 VMware、Radmin VPN 等多张虚拟网卡，Sentinel 自动探测到的是
   `26.37.52.184`，控制台连不上它，日志会一直刷
   `Failed to fetch metric from <http://26.37.52.184:8724/metric...>`，页面上看不到任何监控数据。
   六个模块现在都固定成 `spring.cloud.sentinel.transport.client-ip: 127.0.0.1`
   （需要时可用环境变量 `SENTINEL_CLIENT_IP` 覆盖）。
2. **8719 被控制台占用**：所以各服务的传输端口从 8720 开始分配。

## 九、数据集（1000 名员工 + 72 个岗位）

根目录下两个 CSV 是平台的基础数据，已经拆成 **13 张表**灌进 `talent_db`，
脚本是 [sql/talent_dataset.sql](sql/talent_dataset.sql)。

### 1. CSV 怎么拆的

| 来源 | 拆出来的表 |
|---|---|
| 员工人才梯队数据集_增强版（1000人21字段）.csv | `emp_employee`（基本信息）、`emp_salary`（薪酬）、`emp_performance`（绩效）、`emp_potential`（潜力）、`resign_warning_record`（流失风险）、`emp_talent_tag`（人才标签）、`emp_skill`（会哪些技能）、`training_record`（已完成培训） |
| 岗位画像表.csv | `position`（岗位画像）、`pos_skill_require`（核心/加分技能）、`pos_course_require`（必备培训） |
| 两个 CSV 共用 | `skill`（技能字典）、`course`（课程字典） |

三条原则：

1. **一个人/一个岗位放不下多行的，单独开表**：薪酬、绩效、潜力、流失风险、人才标签、
   技能、培训记录各占一张，以后加一列不会牵动别的表。
2. **CSV 里用顿号拼的列表全部拆成关联表**：技能标签、已完成培训、核心技能、加分技能、
   必备培训都不再存 `A、B、C` 这种字符串，而是指向 `skill` / `course` 两个字典，
   这样「谁会 Go」「这个岗位还缺什么技能」才查得出来。
3. **员工侧和岗位侧共用同一套技能、课程字典**：两边的技能名才能对得上，做匹配才有意义。

### 2. 灌数据

```bat
cmd /c "D:\Develop\MySQL80\bin\mysql.exe -uroot -p20050312 --default-character-set=utf8mb4 < sql\talent_dataset.sql"
```

执行完脚本会自己打印每张表的条数，正常是：

| 表 | 条数 | 表 | 条数 |
|---|---|---|---|
| `skill` | 172 | `emp_employee` | 1000 |
| `course` | 58 | `emp_salary` | 1000 |
| `position` | 72 | `emp_performance` | 1000 |
| `pos_skill_require` | 1437 | `emp_potential` | 1000 |
| `pos_course_require` | 749 | `resign_warning_record` | 1000 |
| `emp_skill` | 4515 | `emp_talent_tag` | 1000 |
| `training_record` | 2528 | | |

> 脚本会 DROP 再重建这些表，`talent_db.sql` 里那 3 条示例数据（E001/E002/E003 和 3 个示例岗位）
> 会被数据集取代。如果这些表里已经录过手工数据，先备份再执行。

### 3. 几个字段的说明

- 数据集只给了「绩效评分」一个分数，没有年份，统一记为 **2026 年度**（`emp_performance.perf_year`）。
- `emp_employee` 里的 `hire_date`、`education`、`phone` 三列 CSV 没提供，留空待补。
- `position` 是从骨架的同名表演化来的，补上了岗位画像里的职级、绩效/司龄要求、是否管理岗等列；
  原来的 `is_key`（关键岗位）保留着，数据集没有给值，需要时由梯队模块自己标注。
- 只有一个员工没有上级（首席执行官），`manager_emp_no` 为空。

### 4. 常用查询

```sql
-- 一个员工的全貌
SELECT e.emp_no, e.name, p.position_name, s.base_salary,
       f.score AS 绩效, po.potential_level AS 潜力, w.warning_level AS 流失风险
FROM emp_employee e
JOIN position p ON p.id = e.position_id
LEFT JOIN emp_salary s ON s.employee_id = e.id
LEFT JOIN emp_performance f ON f.employee_id = e.id
LEFT JOIN emp_potential po ON po.employee_id = e.id
LEFT JOIN resign_warning_record w ON w.employee_id = e.id
WHERE e.emp_no = 'E20612044';

-- 谁会 Go
SELECT e.emp_no, e.name, e.department
FROM emp_employee e
JOIN emp_skill es ON es.employee_id = e.id
JOIN skill sk ON sk.id = es.skill_id
WHERE sk.skill_name = 'Go';

-- 员工离他岗位的要求还差多少技能（缺口越大越需要培训）
SELECT e.name AS 员工, p.position_name AS 岗位,
       COUNT(DISTINCT r.skill_id) AS 岗位要求技能数,
       COUNT(DISTINCT es.skill_id) AS 已具备数
FROM emp_employee e
JOIN position p ON p.id = e.position_id
JOIN pos_skill_require r ON r.position_id = p.id
LEFT JOIN emp_skill es ON es.employee_id = e.id AND es.skill_id = r.skill_id
GROUP BY e.id
ORDER BY 已具备数 - 岗位要求技能数
LIMIT 20;
```
