# Dify 员工档案智能体

这两个 YAML 用来在 Dify 里搭建「员工档案智能管理」智能体：

| 文件 | 用途 |
|---|---|
| `talent-employee-agent.yml` | Dify Chatflow（工作流）DSL，可直接导入 |
| `talent-employee-openapi.yml` | 后端 API 的 OpenAPI 3.0 定义，可导入为 Dify 自定义工具 |

数据分析与报告模块使用独立的 Dify 应用，配置方式见
[数据分析智能体使用说明.md](./数据分析智能体使用说明.md)。

## 一、工作流是怎么工作的

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

> Docker 里的 Dify 访问 `127.0.0.1` 指的是容器自己，不是你的电脑。

如果使用 Docker 版 Dify 且容器无法解析 `host.docker.internal`，将
`dify/docker-compose.override.example.yml` 复制为 Dify 安装目录下的
`docker/docker-compose.override.yml`。同时在 Dify 的 `docker/.env` 中加入：

```env
SSRF_PROXY_ALLOW_PRIVATE_IPS=192.168.64.1/32
SSRF_PROXY_ALLOW_PRIVATE_DOMAINS=host.docker.internal
```

然后执行 `docker compose up -d api api_websocket worker ssrf_proxy`。
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
| 工作流返回连接失败 | 检查 `base_url`，Docker 部署时不能用 `127.0.0.1` |
| LLM 节点报模型不存在 | 换成你 Dify 里已配置的模型 |
| 规划结果是自然语言而不是 JSON | 模型能力不足或温度过高；换模型、把温度降到 0.2 以下 |
| 删除操作没执行 | 设计如此：需要用户明确说「确认删除」 |
| 绩效趋势为空 | 该员工只有单年数据，至少录入两个年份 |
| 潜力接口 404 | 员工服务需要重启加载新 Controller，网关需要重启加载新路由 |
| 前端提示“尚未连接 Dify” | 在启动员工服务的终端设置 `DIFY_BASE_URL`、`DIFY_API_KEY`，然后重启员工服务 |
| 前端提示 HTTP 401 | Dify API Key 错误或不是该应用的密钥，重新复制应用 API Key |
| 错误中同时出现 `[models]` 和 `Authentication Fails` | 这不是应用 API Key 的问题；到 Dify「设置 → 模型供应商」更换模型自己的 API Key |
| 工作流调用后端超时 | 在 Dify 容器里测试 `base_url`，并确认 SSRF 代理已放行 Windows 主机地址 |
