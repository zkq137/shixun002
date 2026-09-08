# 智能企业人才梯队建设平台（微服务骨架）

基于 **Spring Boot 3.2 + Spring Cloud 2023 + Spring Cloud Alibaba + Nacos** 的多模块微服务工程，5 人各负责一个业务模块。

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

## 七、接数据库说明

骨架默认把数据库配置**注释掉了**，方便先跑通服务注册。接入数据库时：

1. 确保已执行 `sql/talent_db.sql`。
2. 取消 `application.yml` 中 `datasource` 注释，按实际账号密码修改。
3. 在 `com.talent.<module>.mapper` 下添加 MyBatis-Plus Mapper 接口，并在 `entity` 包下加实体。
