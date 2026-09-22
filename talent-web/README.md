# talent-web —— 前端

Vue 3 + Vite + TypeScript + Element Plus，对接后端网关（`http://127.0.0.1:9090`）。

## 快速开始

```bash
# 1. 先装依赖（只用做一次）
npm install

# 2. 启动后端（另开一个窗口，在项目根目录执行）
powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1

# 3. 启动前端
npm run dev
```

浏览器打开 http://127.0.0.1:5173

## 页面

| 路由 | 页面 | 对接接口 |
|---|---|---|
| `/dashboard` | 数据分析与报告（5 个页签：梯队整体状态 / 人才流动趋势 / 模型效果与日志 / 自定义报表 / 基础盘点） | `GET /api/analysis/pipeline`、`/flow-trend`、`/model/effect`、`/report/generate` 等 |
| `/employee` | 员工档案：查询、详情、增删改、**批量修改/删除、Excel 导入导出、配技能** | `GET /api/employee/page` 等 |
| `/skill` | 技能体系：分类树 + 子技能增删改查 | `GET /api/skill/tree` 等 |
| `/planning` | 梯队规划（继任候选、风险名单、技能覆盖） | `GET /api/planning/succession` 等 |
| `/training` | 智能培训（学习计划） | `GET /api/training/paths` |
| `/promotion` | 晋升决策（候选人） | `GET /api/promotion/candidates` |

## 员工档案页的几个功能点

- **批量操作**：表格左边勾选多行 → 「批量修改」（只改填了的字段）或「批量删除」。
- **导出 Excel**：导出的是**当前筛选条件下的全部数据**，不只是当前页。实现方式是把查询条件拼成 URL
  直接 `window.open`，让浏览器下载（后端 `/api/employee/export`）。
- **导入 Excel**：弹窗里用 `el-upload` 直接 POST 到 `/api/employee/import`，成功/失败明细在弹窗里按行号展示。
  这个接口没走 axios 封装，因为文件上传要用 multipart。
- **配技能**：点某行的「技能」按钮，弹出技能树勾选（`el-tree` + `show-checkbox`），
  保存时只提交叶子节点（`getCheckedNodes(true)`），分类节点不会被当成技能。

## 目录结构

```
src/
├── api/            一个后端模块一个文件，request.ts 负责统一拆包
│   ├── request.ts  axios 实例：baseURL=/api，自动拆 Result、失败弹提示
│   ├── types.ts    后端返回的数据类型，改接口时先改这里
│   └── employee.ts / planning.ts / training.ts / promotion.ts / analysis.ts
├── router/index.ts 路由表，菜单和页面在这里对应
├── views/          5 个页面，一个模块一个文件
├── App.vue         整体布局：左侧菜单 + 顶栏 + <router-view>
└── main.ts         挂载 Element Plus、图标、路由
```

## 几个约定

**接口地址不用写全**。`request.ts` 里 `baseURL = '/api'`，`vite.config.ts` 里配了代理把
`/api` 转发到网关 9090，所以页面里只写 `get('/employee/page')`，既不用管跨域，也不用改后端地址。

**返回格式已经统一处理**。后端返回 `{code, message, data}`，`request.ts` 在 `code === 200`
时直接把 `data` 交给调用方，否则弹错误提示并抛异常。所以页面里直接写：

```ts
const page = await fetchEmployeePage(query)   // page 就是 { total, records }
```

**加一个新页面的步骤**：`views/` 加一个 `.vue` → `api/` 加一个请求函数 →
`router/index.ts` 加一条路由 → `App.vue` 菜单加一个 `el-menu-item`。

## 生产构建

```bash
npm run build     # 产物在 dist/
npm run preview   # 本地预览构建结果
```

`dist/` 是纯静态文件，交给 Nginx 之类的服务器托管即可。生产环境要让 Nginx 把 `/api`
反向代理到网关（开发环境是 Vite 的 proxy 在做这件事）：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:9090/api/;
}
location / {
    try_files $uri $uri/ /index.html;   # 前端是 history 路由，刷新子页面要回落到 index.html
}
```

## 已知可优化项

- Element Plus 目前是全量引入（主包约 866 KB），要瘦身可以改成按需引入。
- 还没有登录和权限控制，所有页面都是公开的。
- 列表页没做导出 Excel、批量操作。
