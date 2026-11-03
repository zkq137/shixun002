# Dify 智能体工作流

下表中的 YAML 用于在 Dify 里搭建人才智能体：

| 文件 | 用途 |
|---|---|
| `talent-employee-agent.yml` | 员工档案智能体，后端注入只读员工上下文 |
| `talent-analysis-agent.yml` | 数据分析与报告智能体，后端注入只读分析上下文 |
| `talent-planning-agent.yml` | 梯队规划智能体，后端注入只读规划上下文 |
| `talent-promotion-agent.yml` | Dify Chatflow（工作流）DSL，综合晋升候选池、梯队继任评估和技能体系数据 |
| `talent-training-agent.yml` | 智能培训推荐智能体，后端注入员工培训画像、岗位必修、技能缺口与培训效果 |
| `talent-employee-openapi.yml` | 后端 API 的 OpenAPI 3.0 定义，可导入为 Dify 自定义工具 |

当前员工、分析、规划、晋升、培训五条链路都采用 **后端注入上下文** 模式。浏览器只访问本地
网关，业务服务读取本地数据库并把裁剪后的只读快照发给 Dify Cloud；Dify 不再回调本地
接口，因此不需要公网 IP、端口映射、隧道或 `base_url`。

```text
浏览器 → 本地网关 → 业务服务 → Dify Cloud（仅出站 HTTPS）
                         └→ 本地数据库与规则计算
```

| 应用 | Start 输入变量 | 数据来源 |
|---|---|---|
| 员工档案 | `query`、`employee_context` | 员工档案、绩效、潜力、技能、部门与岗位 |
| 数据分析 | `query`、`analysis_context` | 看板、梯队健康度、流动趋势、预测、模型与报表 |
| 梯队规划 | `query`、`planning_context` | 岗位、继任、技能覆盖与规划规则 |
| 晋升决策 | `query`、`promotion_context` | 晋升候选池、继任、技能覆盖与规则评分 |
| 智能培训 | `query`、`training_context` | 员工培训画像、岗位必修、已完成培训、技能缺口、学习路径与课程效果 |

各模块工作流只保留「开始 → LLM → 答案」三个节点，工作流环境变量为空。Dify 仅负责
根据后端提供的证据生成解释、建议和风险提示，不执行新增、修改、删除、导入或模型训练。

> 下文「旧版 HTTP Request 模式」及 OpenAPI/Excel 章节仅保留作历史排错参考，不应再按
> 其中的 `base_url`、SSRF 放行或公网隧道步骤部署员工/分析工作流。

## 一、旧版 HTTP Request 模式（已废弃）

```
用户输入
  │
  ├─ LLM 意图解析：把自然语言转成 {method, path, body, summary}
  ├─ Code 节点：清洗 JSON、规范化 method/path/body
  ├─ If/Else 链：按 GET / POST / PUT / DELETE 分流
  ├─ HTTP Request：调用网关 http://<base_url>/api/...
  └─ Answer：返回执行结果
```

覆盖的能力：

- 员工：分页查询、详情、新增、修改、删除、批量删除、批量修改、部门/岗位下拉、部门统计
- 员工技能：查询、覆盖设置
- 技能树：整树、分类、子技能、搜索、新增/修改/删除
- 历年绩效：历史、趋势、跨年对比、单条/批量录入、修改、删除
- 潜力评估：模型定义、模型初始化、查询、单个/批量录入
- 员工定位：数字 ID、完整姓名和工号都可以；姓名或工号会由员工服务先解析成唯一 ID

安全规则（写在 LLM 系统提示里）：

- 删除/批量删除必须用户明确说「确认删除」才会调用 DELETE
- 缺员工 ID、年份等关键信息时不会瞎猜，会先追问
- Excel 导入/导出走专门说明（见下文）

## 二、导入前准备

1. **启动后端**（网关 9090、员工服务 8081，Nacos/MySQL 视部署方式而定）
2. 确认网关能访问：

```bash
curl http://127.0.0.1:9090/api/employee/page?pageNum=1&pageSize=1
```

3. 确认 Dify 能访问这个地址——**这是最容易错的一步**：

> 工作流默认使用 **`http://host.docker.internal:9090`**。本机 Dify 已通过
> `docker-compose.override.yml` 将该域名映射到 Windows 主机，并放行这个私有地址；
> 已从 Dify 容器内实测返回 HTTP 200。

| Dify 部署方式 | `base_url` 应该填 |
|---|---|
| Dify 与后端在同一台机器、直接进程启动 | `http://127.0.0.1:9090` |
| Dify 用 Docker 部署（已配置主机映射） | `http://host.docker.internal:9090` |
| 后端在另一台机器 | `http://后端IP:9090` |
| 使用共享环境 | `http://10.1.249.82:9090` 或 Radmin `http://26.37.52.184:9090` |
| Dify Cloud（`api.dify.ai`） | 公网可访问的 HTTPS 网关地址，不能填本机、Docker 或内网地址 |

> Docker 里的 Dify 访问 `127.0.0.1` 指的是容器自己，不是你的电脑。

> Dify Cloud 的工作流 HTTP 请求由云端发起，因此
> `127.0.0.1`、`host.docker.internal`、局域网 IP 和 Radmin IP 都不可达。
> 使用 Cloudflare Tunnel、ngrok 或公网反向代理把网关 `9090` 暴露成 HTTPS 地址后，
> 再把该地址填入工作流环境变量 `base_url`。

Docker 版 Dify 访问本机或内网后端时，需要在 SSRF 代理中放行目标私有地址。在 Dify
安装目录的 `docker/.env` 中加入（后端与 Dify 同机时）：

```env
SSRF_PROXY_ALLOW_PRIVATE_IPS=<Dify 容器内 host.docker.internal 解析出的主机网关IP>/32
SSRF_PROXY_ALLOW_PRIVATE_DOMAINS=host.docker.internal
```

如果 `host.docker.internal` 无法解析，将
`dify/docker-compose.override.example.yml` 复制为 Dify 安装目录下的
`docker/docker-compose.override.yml`。示例使用 Docker 的 `host-gateway` 动态映射，
不依赖某台电脑固定的网关 IP。然后在 Dify 的 `docker` 目录执行
`docker compose exec ssrf_proxy getent hosts host.docker.internal` 获取当前机器的网关 IP，
将其填入 `SSRF_PROXY_ALLOW_PRIVATE_IPS`。若后端在另一台电脑，则将 `base_url` 设为该电脑
的可达 IP，并将 `SSRF_PROXY_ALLOW_PRIVATE_IPS` 配置为该后端 IP 的 `/32` 网段。
配置后执行 `docker compose up -d api api_websocket worker ssrf_proxy`。
Dify 的 HTTP 节点会经过 SSRF 代理，只有主机映射但未放行私有地址时仍然会报
`Reached maximum retries`。

## 三、导入工作流

1. Dify → **工作室（Studio）** → 创建应用 → **导入 DSL 文件**
2. 选择 `talent-employee-agent.yml`
3. 打开工作流，修改两处：
   - **环境变量** `base_url`：改成上表对应的地址
   - **LLM 节点**的模型：DSL 里默认写的是 `openai / gpt-4o-mini`，换成你 Dify 里已配置的模型
4. 发布（Publish）

DSL 版本是 `0.1.5`，Dify 1.x 会向后兼容并自动升级。如果你的 Dify 版本很老，导入失败时把 `version` 改成你实例支持的值（Dify 会在报错里提示）。

## 四、试用示例

```
查询研发中心前 10 名员工
查看龙秀英的绩效趋势
新增员工：工号 E90000001，姓名张三，岗位为市场经理
查看工号 E70014679 的员工详情
新增一名员工：工号 E90000001，姓名张三，岗位ID 3，部门研发中心
把员工 1 的 2025 年绩效改成 4.2 分
看一下员工 1 的绩效趋势
对比员工 1 的 2025 和 2026 年绩效
初始化潜力评估指标
列出技能树里的研发技术分类
```

新增员工最少只需要 **工号、姓名、岗位名称**。员工服务会自动把岗位名称解析成
岗位 ID，并补上岗位所属部门；性别、年龄、学历、手机号、办公方式等都可以省略。

危险操作会先被拦下来：

```
用户：删除员工 1
助手：删除会同时清理薪酬/绩效/技能等关联数据，请回复「确认删除」后我再执行。

用户：确认删除员工 1
助手：执行 DELETE /api/employee/1 ...
```

## 五、Excel 导入/导出的说明

工作流对 Excel 做了保守处理：

- **导出**：LLM 会返回可点击的下载链接，例如
  `http://<base_url>/api/employee/export?department=研发中心`，
  浏览器打开即可下载；绩效导出同理。
- **导入**：目前建议在「员工档案」页面使用「导入 Excel」，或在 Dify 里改用自定义工具方式（见第六节）。
  原因是 Dify 的 multipart 文件上传节点在不同版本里差异较大，直接写死在 DSL 里容易导入失败。

如果一定要在 Dify 里做文件导入，可以在工作流开始节点加一个 `file` 变量，再接一个 `body.type: form-data` 的 HTTP 节点，字段名用 `file`，目标接口：

```
POST http://<base_url>/api/employee/import
POST http://<base_url>/api/performance/import
```

## 六、可选：用 OpenAPI 方式做更自由的 Agent

如果希望 Agent 自己选择接口（而不是固定四类 HTTP 节点），可以：

1. Dify → 工具 → 自定义工具 → 创建 → 导入 `talent-employee-openapi.yml`
2. 把 Server URL 改成对应的 `base_url`
3. 创建 **Agent** 应用，挂载这个工具
4. 系统提示词可以直接复用工作流里「解析操作意图」节点的内容

OpenAPI 文件包含 26 个路径、31 个操作，已覆盖员工、技能树、绩效、潜力四块。

## 七、后端接口变更（本次新增）

为了让上面的能力可用，员工模块新增了：

- `GET/POST/PUT/DELETE /api/performance/...`：历年绩效、趋势、跨年对比、批量录入
- `POST /api/performance/import`、`GET /api/performance/import-template`
- `GET/POST /api/potential/...`：潜力模型、模型初始化、查询、单个/批量录入

网关新增路由：

```yaml
- id: talent-performance
  uri: lb://talent-employee-service
  predicates: [ Path=/api/performance/** ]
- id: talent-potential
  uri: lb://talent-employee-service
  predicates: [ Path=/api/potential/** ]
```

## 八、接入前端 AI 助手

前端侧边栏已经增加「AI 助手」页面（`/assistant`）。浏览器只调用本项目的
`/api/ai/chat`，不会拿到 Dify API Key；由员工服务在服务端调用 Dify：

```
浏览器 → Vite /api 代理 → 网关 9090 → 员工服务 8081 → Dify /v1/chat-messages
```

### 1. 启动前设置环境变量

在启动员工服务的同一个 PowerShell 窗口中执行：

```powershell
$env:DIFY_BASE_URL="https://api.dify.ai"
$env:DIFY_API_KEY="app-你的应用API密钥"
```

- 自建 Dify 时，`DIFY_BASE_URL` 改成 Dify 的访问地址，例如 `http://127.0.0.1`。
- 只填 Dify 根地址或带 `/v1` 的地址都可以，后端会自动补全 `/v1/chat-messages`。
- 不要把这些值写进前端文件；API Key 只应保存在服务端环境变量或 Nacos 加密配置中。
- 设置后必须重新启动员工服务，Dify 配置才会生效。

项目一键启动方式：

```powershell
$env:DIFY_BASE_URL="https://api.dify.ai"
$env:DIFY_API_KEY="app-你的应用API密钥"
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-all.ps1 -SkipSentinel
```

如果员工服务已经在运行，先停止全部业务服务再按上面的方式启动：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\stop-all.ps1 -KeepNacos -KeepSentinel
```

### 2. 验证连接状态

```powershell
Invoke-RestMethod http://127.0.0.1:9090/api/ai/status
```

返回 `configured: true` 表示员工服务已经读取到 Dify 地址和 API Key。返回
`configured: false` 时，检查环境变量是否设置在启动员工服务的终端中，并确认服务已重启。

### 3. 在前端对话

1. 启动前端：在 `talent-web` 目录执行 `npm run dev`。
2. 打开 http://127.0.0.1:5173/assistant 。
3. 页面显示「已连接 Dify」后，可以输入“查询研发中心前 10 名员工”等测试问题。
4. 第一轮成功后，后端会保存 `conversation_id`，同一页面内后续问题可继续使用上下文。

Dify 工作流必须已经 **发布**。如果提示 Dify 没有返回 `answer`，检查导入的是否为
Chatflow 应用，以及工作流最后是否连接到 Answer 节点。

## 九、常见问题

| 现象 | 处理 |
|---|---|
| 导入 DSL 报版本错误 | 修改 YAML 顶部的 `version` 为实例支持版本 |
| LLM 节点报模型不存在 | 换成你 Dify 里已配置的模型 |
| 绩效趋势为空 | 该员工只有单年数据，至少录入两个年份 |
| 前端提示“尚未连接 Dify” | 在启动员工服务的终端设置 `DIFY_BASE_URL`、`DIFY_API_KEY`，然后重启员工服务 |
| 前端提示 HTTP 401 | Dify API Key 错误或不是该应用的密钥，重新复制应用 API Key |
| 错误中同时出现 `[models]` 和 `Authentication Fails` | 这不是应用 API Key 的问题；到 Dify「设置 → 模型供应商」更换模型自己的 API Key |
| Start 节点缺少 `employee_context` / `analysis_context` / `training_context` | 云端仍是旧 DSL；重新导入并发布对应的新版 YAML，不需要新增环境变量 |
| Dify 返回 502，或日志出现 `host.docker.internal` | 云端仍在执行旧 HTTP Request 节点；导入新版出站-only DSL 后重新发布 |
| 回答总是说没有数据 | 确认后端调用 Dify 时携带了对应上下文变量，并检查员工/分析服务日志中的上下文构建异常 |
| 要求新增、修改、删除或训练但没有执行 | 当前员工与分析助手是只读分析链路；写操作必须走现有管理页面或后端受控接口 |

## 十、晋升决策工作流（talent-promotion-agent.yml）

该 Chatflow 使用「后端注入上下文」模式：由本地晋升服务读取晋升候选池、梯队继任、
岗位技能覆盖、员工档案和技能明细，完成规则评分后把 `promotion_context` 推送给
Dify Cloud。Dify 不再访问本地接口，因此不需要开放入站端口、公网 IP 或隧道。

```
用户输入
  → 晋升服务解析目标岗位/候选人
  → 本地查询晋升候选池、继任候选人、岗位技能覆盖
  → 本地合并规则分与重点候选人档案
  → 后端 POST Dify /v1/chat-messages（携带 promotion_context）
  → Dify LLM 生成决策说明
  → Answer
```

请求链路：

```text
浏览器 → 本地网关 9090 → 晋升服务 8084 → 本地其他业务接口
                                      └→ https://api.dify.ai（仅出站）
```

依赖接口：

| 接口 | 用途 |
|---|---|
| `GET /api/planning/positions` | 岗位列表与岗位匹配 |
| `GET /api/promotion/candidates` | 跨层级且 2026 绩效≥4 的晋升候选池 |
| `GET /api/planning/succession` | 岗位继任匹配度、准备度与技能缺口 |
| `GET /api/planning/skill-coverage` | 岗位技能要求及全组织覆盖情况 |
| `GET /api/employee/referenced` | 从自然语言问题中解析员工姓名或工号，得到稳定员工 ID |
| `GET /api/employee/{id}` | 重点候选人档案 |
| `GET /api/employee/{id}/skills` | 重点候选人技能明细 |
| `GET /api/performance/employees/{id}` | 员工历年绩效 |
| `GET /api/performance/employees/{id}/trend` | 员工绩效趋势、均值、同比变化 |

规则分口径：

```text
decisionScore = 0.40 × 晋升分 + 0.35 × 继任匹配度
              + 0.15 × 准备度 + 0.10 × 风险分
```

- 晋升分按 `totalScore / 150 × 100` 归一化，并限制在 0–100。
- `READY_NOW/READY_1_YEAR/READY_2_YEARS/NOT_READY` 分别折算为 100/85/55/20。
- 低/中/高风险分别折算为 100/60/30。
- 排序先按 `evidenceCoverage` 分组（`full` 优先），同组再按 readiness
  `READY_NOW > READY_1_YEAR > READY_2_YEARS > NOT_READY` 和 `decisionScore` 排序。
- `READY_2_YEARS`、`NOT_READY`，或核心技能匹配为 0/技能分低于 40 的候选人，
  最高只标记为「进入继任池，暂不晋升」，不会直接给出晋升推荐。
- 若某人不在一岗的继任池中，`evidenceCoverage` 标记为 `promotion_only`，使用
  `0.80 × 晋升分 + 0.20 × 风险分`，结论只能写「待补充继任评估」，不能直接判定不匹配。
- 当晋升候选池与继任池交集为 0 时，证据中会写入 `successionOverlapCount = 0`，
  LLM 必须提示两池无交集，仅给硬条件符合名单，不直接下晋升结论。

### 接入后端与前端

晋升服务使用独立的 Dify 配置，避免覆盖员工档案助手：

```powershell
$env:PROMOTION_DIFY_BASE_URL = "https://api.dify.ai"
$env:PROMOTION_DIFY_API_KEY = "app-晋升应用API密钥"
```

- 后端代理接口：`GET /api/promotion/ai/status`、`POST /api/promotion/ai/chat`。
- 浏览器只访问后端接口，不会拿到 API Key。
- 前端 `http://127.0.0.1:5173/promotion` 底部已接入「晋升决策智能体」。
- 项目根目录的 `.shared-env.ps1` 会被 `scripts/start-all.ps1` 自动加载；该文件已在
  `.gitignore` 中，只适合本机保存密钥，不要提交到仓库。

验证配置：

```powershell
Invoke-RestMethod http://127.0.0.1:9090/api/promotion/ai/status
```

晋升服务读取本地模块数据时默认访问 `http://127.0.0.1:9090`。如果部署方式不同，
只在本机启动晋升服务前设置：

```powershell
$env:TALENT_GATEWAY_URL = "http://127.0.0.1:9090"
```

导入新版 `talent-promotion-agent.yml` 后：

1. 工作流环境变量应为空，不需要 `base_url`。
2. Start 节点应包含 `query` 和 `promotion_context` 两个输入变量。
3. 图中只有「开始 → LLM → 答案」三个节点，不应再有 HTTP 请求节点。
4. LLM 节点默认使用 `DeepSeek / deepseek-v4-flash`，可换成当前实例已配置的模型。
5. 发布应用后试问：

```text
评估市场经理岗位的晋升候选人
评估张三晋升市场经理的可能性
给出招聘专员岗位的技能差距和培养建议
```

注意：晋升候选池是「硬条件候选池」，继任池是「岗位匹配池」，两者口径不同；
`decisionScore` 仅是规则辅助分，最终晋升仍需结合面谈、编制和人事审批。后端会过滤
手机号、薪酬、年龄和性别等字段，但员工姓名、绩效、潜力与技能数据仍会发送到 Dify
Cloud；若数据不能出内网，应改用自建 Dify。

按员工姓名提问（例如「查看龙秀英的绩效趋势」）时，后端会先调用
`/api/employee/referenced` 解析员工 ID，再注入员工档案、历年绩效和趋势。若数据集中
只有单年度绩效，模型应明确说明“仅有该年度数据，无法判断多年趋势”，而不是回答没有员工数据。

## 十一、智能培训推荐工作流（talent-training-agent.yml）

该 Chatflow 同样采用「后端注入上下文」模式。培训服务会在本地读取员工培训画像、
岗位必备课程、已完成培训、技能缺口、学习路径模板和课程效果，再把裁剪后的
`training_context` 发送给 Dify。Dify 不回调本地网关。

   用户输入
     → 培训服务解析员工姓名/工号或岗位名称
     → 本地计算岗位必修 − 已完成培训
     → 后端 POST Dify /v1/chat-messages（携带 training_context）
     → Dify LLM 生成学习优先级与分阶段计划
     → Answer

导入 `talent-training-agent.yml` 后：

> 节点 ID 和 `value_selector` 只能使用字母、数字、下划线。不要改成带连字符的 ID，
> 否则 Dify 会把 `{{#training_start.query#}}` 原样传给模型，前端会收到未解析模板。
> 若应用已经导入过旧版本，需要重新导入本文件并发布，现有 API Key 可以继续使用。

1. 工作流环境变量应为空，不需要 `base_url`。
2. Start 节点应包含 `query` 和 `training_context` 两个输入变量。
3. 图中只有「开始 → LLM → 答案」三个节点，不应出现 HTTP 请求节点。
4. LLM 节点默认使用 `DeepSeek / deepseek-v4-flash`，可换成当前实例已配置的模型。
5. 在启动培训服务前设置应用密钥并重启：

    $env:TRAINING_DIFY_BASE_URL = "https://api.dify.ai"
    $env:TRAINING_DIFY_API_KEY = "app-培训应用API密钥"

6. 验证配置与对话接口：

    Invoke-RestMethod http://127.0.0.1:9090/api/training/ai/status

7. 发布应用后在前端「智能培训」页面试问：

    查看龙秀英的培训推荐和技能缺口
    市场经理岗位需要哪些必备培训
    为研发中心员工制定分阶段学习路径
    分析培训参与率和通过率最高的课程

个人推荐只使用 `trainingProfiles.trainingPlan.recommendedCourses`，不会重复推荐
`completedCourses`。岗位级要求来自 `positionTrainingRequirements`，组织级课程效果
来自 `trainingEffects`，两者不能混作个人完成情况或个人绩效。
