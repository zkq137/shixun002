from copy import deepcopy
from pathlib import Path

import yaml


ROOT = Path(__file__).resolve().parents[1]
DIFY_DIR = ROOT / "dify"
BASE_FILE = DIFY_DIR / "talent-employee-agent.yml"
WORKFLOW_FILE = DIFY_DIR / "talent-analysis-agent.yml"
OPENAPI_FILE = DIFY_DIR / "talent-analysis-openapi.yml"


PLANNER_PROMPT = """你是「企业人才数据分析与报告」智能体。
你的任务：把用户的自然语言需求转换成一个后端 API 调用计划。

只输出一个 JSON 对象，不要输出 Markdown、解释或多余文字。
JSON 格式：
{"method":"GET|POST|DELETE|NONE","path":"/api/analysis/...","body":{},"summary":"一句话说明要做什么"}

规则：
1. GET 查询参数直接拼在 path 里。
2. POST 请求体放在 body 对象中。
3. 删除报表或模板属于危险操作。只有用户明确说“确认删除”时才输出 DELETE，否则 method=NONE 并请用户确认。
4. 报表生成只能使用 options 中的维度/指标 key；未给指标时默认 count。
5. 同名报表模板没有修改接口。用户要改模板时，请先删除旧模板并重新保存，必须明确确认删除。
6. 当前没有人才流入/流出/晋升的预测模型接口，也没有预测数据导入导出接口。
   用户要求这些功能时，method=NONE，并说明当前只能分析历史流动趋势，不能伪造预测结果。
7. 当前没有仪表盘配置导出、报表模板导入接口。遇到这些需求时 method=NONE 并说明限制。
8. 不要编造接口，不要声称执行了未调用接口的操作。

可用接口：
【人才盘点与梯队健康度】
GET /api/analysis/dashboard
GET /api/analysis/pipeline
GET /api/analysis/flow-trend

【自定义报表】
GET  /api/analysis/report/options
POST /api/analysis/report/generate
     body: {reportName,dimensions:[],metrics:[],filters:{},save:false}
POST /api/analysis/report/from-template/{templateId}
GET  /api/analysis/report/history
GET  /api/analysis/report/templates
POST /api/analysis/report/templates
     body: {reportName,dimensions:[],metrics:[],filters:{},save:false}
DELETE /api/analysis/report/templates/{id}
GET  /api/analysis/report/{id}
DELETE /api/analysis/report/{id}

【模型运行效果与日志】
GET /api/analysis/model/effect
GET /api/analysis/model/logs?modelName=&status=&version=&pageNum=1&pageSize=10
GET /api/analysis/model/logs/{id}
GET /api/analysis/model/names

报表维度 key：
department, levelTier, position, positionType, potentialLevel, talentTag,
warningLevel, workMode, gender, ageRange, tenureRange, perfLevel

报表指标 key：
count, avgAge, avgTenure, avgSalary, avgPerf, avgPotential, avgRiskScore,
highRiskCount, middleRiskCount, coreTalentCount, reserveTalentCount,
highPotentialCount

常用报表示例：
- 部门人数和平均绩效：
  {"dimensions":["department"],"metrics":["count","avgPerf"]}
- 人才标签与流失风险交叉：
  {"dimensions":["talentTag","warningLevel"],"metrics":["count"]}
- 高风险员工部门分布：
  {"dimensions":["department"],"metrics":["count","highRiskCount"],
   "filters":{"warningLevel":"高风险"}}
"""


ANALYST_PROMPT = """你是企业人力资源战略数据分析专家。
你会收到用户的原始问题和后端接口返回的真实 JSON。

请输出中文分析结论，要求：
1. 先给出 2-4 条最重要的发现，引用具体数字。
2. 按“整体状态、结构、风险、趋势、模型效果、行动建议”中与数据相关的部分展开。
3. 区分事实和推断；推断必须写明依据，禁止编造数据中不存在的信息。
4. 如果返回 code 不是 200，直接解释错误原因和修正建议。
5. 输出适合业务人员阅读，使用简短标题和项目符号，不要输出原始 JSON。
6. 如果用户询问未来预测，只能基于历史趋势给出观察和情景判断，并明确说明这不是预测模型结果。
7. 不得复述“系统预校验”文本，也不得输出 method/path/body 调用计划。
"""


def custom_node(data, node_id, x, y, width=244, height=100):
    return {
        "data": data,
        "height": height,
        "id": node_id,
        "position": {"x": x, "y": y},
        "positionAbsolute": {"x": x, "y": y},
        "selected": False,
        "sourcePosition": "right",
        "targetPosition": "left",
        "type": "custom",
        "width": width,
    }


def edge(source, target, source_type, target_type, source_handle="source"):
    return {
        "data": {
            "isInIteration": False,
            "sourceType": source_type,
            "targetType": target_type,
        },
        "id": f"{source}-{source_handle}-{target}-target",
        "source": source,
        "sourceHandle": source_handle,
        "target": target,
        "targetHandle": "target",
        "type": "custom",
        "zIndex": 0,
    }


def if_node(node_id, title, method, condition_id, x, y):
    return custom_node(
        {
            "cases": [
                {
                    "case_id": "true",
                    "conditions": [
                        {
                            "comparison_operator": "is",
                            "id": condition_id,
                            "value": method,
                            "varType": "string",
                            "variable_selector": ["parse", "method"],
                        }
                    ],
                    "id": "true",
                    "logical_operator": "and",
                }
            ],
            "desc": "",
            "selected": False,
            "title": title,
            "type": "if-else",
        },
        node_id,
        x,
        y,
        height=126,
    )


def http_node(method, x, y):
    node_id = f"http_{method.lower()}"
    body = (
        {"data": "{{#parse.body#}}", "type": "json"}
        if method == "POST"
        else {"data": [], "type": "none"}
    )
    return custom_node(
        {
            "authorization": {"config": None, "type": "no-auth"},
            "body": body,
            "desc": "",
            "headers": "Content-Type:application/json" if method == "POST" else "",
            "method": method.lower(),
            "params": "",
            "selected": False,
            "timeout": {
                "max_connect_timeout": 0,
                "max_read_timeout": 0,
                "max_write_timeout": 0,
            },
            "title": f"{method} 分析接口",
            "type": "http-request",
            "url": "{{#env.base_url#}}{{#parse.path#}}",
            "variables": [],
        },
        f"http_{method.lower()}",
        x,
        y,
        height=110,
    )


def llm_node(node_id, title, system_prompt, source_node, x, y):
    return custom_node(
        {
            "context": {"enabled": False, "variable_selector": []},
            "desc": "",
            "model": {
                "completion_params": {"temperature": 0.2},
                "mode": "chat",
                "name": "deepseek-v4-flash",
                "provider": "langgenius/deepseek/deepseek",
            },
            "prompt_template": [
                {
                    "id": f"{node_id}-system",
                    "role": "system",
                    "text": system_prompt,
                },
                {
                    "id": f"{node_id}-user",
                    "role": "user",
                    "text": (
                        "用户问题：{{#start.query#}}\n\n"
                        f"接口返回：\n{{{{#{source_node}.body#}}}}"
                    ),
                },
            ],
            "selected": False,
            "title": title,
            "type": "llm",
            "variables": [
                {
                    "value_selector": ["start", "query"],
                    "variable": "query",
                },
                {
                    "value_selector": [source_node, "body"],
                    "variable": "result",
                },
            ],
            "vision": {"enabled": False},
        },
        node_id,
        x,
        y,
        height=98,
    )


def answer_node(node_id, title, text, x, y):
    return custom_node(
        {
            "answer": text,
            "desc": "",
            "selected": False,
            "title": title,
            "type": "answer",
            "variables": [],
        },
        node_id,
        x,
        y,
        height=103,
    )


def build_workflow(source):
    workflow = deepcopy(source)
    workflow["app"] = {
        "description": "人才梯队健康度、流动趋势、自定义报表与模型效果分析",
        "icon": "📊",
        "icon_background": "#EAF4FF",
        "mode": "advanced-chat",
        "name": "数据分析与报告智能体",
        "use_icon_as_answer_icon": False,
    }
    workflow["workflow"]["environment_variables"] = [
        {
            "name": "base_url",
            "value": "http://host.docker.internal:9090",
            "value_type": "string",
        }
    ]
    workflow["workflow"]["features"]["opening_statement"] = (
        "你好，我是人才数据分析助手。可以分析梯队健康度、人才流动趋势、"
        "生成自定义报表、查看模型效果并追溯训练日志。"
    )
    workflow["workflow"]["features"]["suggested_questions"] = [
        "分析当前人才梯队健康度",
        "查看人才流动趋势和主要风险",
        "按部门生成人数和平均绩效报表",
        "查看模型运行效果最好的版本",
    ]

    start = custom_node(
        {
            "desc": "",
            "selected": False,
            "title": "开始",
            "type": "start",
            "variables": [
                {
                    "label": "分析需求",
                    "max_length": 2000,
                    "options": [],
                    "required": True,
                    "type": "text-input",
                    "variable": "query",
                }
            ],
        },
        "start",
        30,
        320,
        width=242,
        height=108,
    )
    planner = llm_node("planner", "解析分析意图", PLANNER_PROMPT, "", 330, 320)
    planner["data"]["prompt_template"][1]["text"] = "用户需求：{{#start.query#}}"
    planner["data"]["variables"] = [
        {"value_selector": ["start", "query"], "variable": "query"}
    ]

    parse = custom_node(
        {
            "code": """import json
import re

def main(plan: str) -> dict:
    text = (plan or "").strip()
    if text.startswith("```"):
        text = re.sub(r"^```[a-zA-Z]*\\s*", "", text)
        text = re.sub(r"\\s*```$", "", text)
    start = text.find("{")
    end = text.rfind("}")
    if start >= 0 and end > start:
        text = text[start:end + 1]
    try:
        data = json.loads(text)
    except Exception:
        data = {}
    method = str(data.get("method", "NONE")).upper()
    if method not in ("GET", "POST", "DELETE", "NONE"):
        method = "NONE"
    path = str(data.get("path", "") or "")
    if path and not path.startswith("/"):
        path = "/" + path
    body = data.get("body") or {}
    if not isinstance(body, dict):
        body = {}
    summary = str(data.get("summary", "") or "")
    return {
        "method": method,
        "path": path,
        "body": json.dumps(body, ensure_ascii=False),
        "summary": summary,
    }
""",
            "code_language": "python3",
            "desc": "",
            "outputs": {
                key: {"children": None, "type": "string"}
                for key in ("method", "path", "body", "summary")
            },
            "selected": False,
            "title": "解析调用计划",
            "type": "code",
            "variables": [
                {"value_selector": ["planner", "text"], "variable": "plan"}
            ],
        },
        "parse",
        630,
        320,
        height=54,
    )

    if_get = if_node(
        "if_get",
        "是否 GET 查询",
        "GET",
        "b1e0f0c1-1a2b-4c3d-8e4f-100000000001",
        930,
        320,
    )
    http_get = http_node("GET", 1230, 120)
    analyst_get = llm_node(
        "analyst_get", "AI 解读分析结果", ANALYST_PROMPT, "http_get", 1530, 120
    )
    answer_get = answer_node(
        "answer_get", "分析结论", "{{#analyst_get.text#}}", 1830, 120
    )

    if_post = if_node(
        "if_post",
        "是否 POST 操作",
        "POST",
        "b1e0f0c1-1a2b-4c3d-8e4f-100000000002",
        930,
        500,
    )
    http_post = http_node("POST", 1230, 500)
    analyst_post = llm_node(
        "analyst_post", "AI 解读报表结果", ANALYST_PROMPT, "http_post", 1530, 500
    )
    answer_post = answer_node(
        "answer_post", "操作分析", "{{#analyst_post.text#}}", 1830, 500
    )

    if_delete = if_node(
        "if_delete",
        "是否 DELETE 操作",
        "DELETE",
        "b1e0f0c1-1a2b-4c3d-8e4f-100000000003",
        930,
        700,
    )
    http_delete = http_node("DELETE", 1230, 700)
    answer_delete = answer_node(
        "answer_delete",
        "删除结果",
        "已执行：{{#parse.summary#}}\n\n返回：\n```json\n{{#http_delete.body#}}\n```",
        1530,
        700,
    )

    noop = custom_node(
        {
            "code": """def main(summary: str) -> dict:
    return {
        "message": summary or "该需求当前无法通过已实现接口完成。"
    }
""",
            "code_language": "python3",
            "desc": "",
            "outputs": {"message": {"children": None, "type": "string"}},
            "selected": False,
            "title": "生成能力提示",
            "type": "code",
            "variables": [
                {"value_selector": ["parse", "summary"], "variable": "summary"}
            ],
        },
        "noop",
        1230,
        880,
        height=54,
    )
    answer_noop = answer_node("answer_noop", "提示", "{{#noop.message#}}", 1530, 880)

    nodes = [
        start,
        planner,
        parse,
        if_get,
        http_get,
        analyst_get,
        answer_get,
        if_post,
        http_post,
        analyst_post,
        answer_post,
        if_delete,
        http_delete,
        answer_delete,
        noop,
        answer_noop,
    ]
    edges = [
        edge("start", "planner", "start", "llm"),
        edge("planner", "parse", "llm", "code"),
        edge("parse", "if_get", "code", "if-else"),
        edge("if_get", "http_get", "if-else", "http-request", "true"),
        edge("http_get", "analyst_get", "http-request", "llm"),
        edge("analyst_get", "answer_get", "llm", "answer"),
        edge("if_get", "if_post", "if-else", "if-else", "false"),
        edge("if_post", "http_post", "if-else", "http-request", "true"),
        edge("http_post", "analyst_post", "http-request", "llm"),
        edge("analyst_post", "answer_post", "llm", "answer"),
        edge("if_post", "if_delete", "if-else", "if-else", "false"),
        edge("if_delete", "http_delete", "if-else", "http-request", "true"),
        edge("http_delete", "answer_delete", "http-request", "answer"),
        edge("if_delete", "noop", "if-else", "code", "false"),
        edge("noop", "answer_noop", "code", "answer"),
    ]
    workflow["workflow"]["graph"] = {"edges": edges, "nodes": nodes}
    return workflow


def response_schema(description):
    return {
        "200": {
            "description": description,
            "content": {
                "application/json": {
                    "schema": {
                        "type": "object",
                        "properties": {
                            "code": {"type": "integer", "example": 200},
                            "message": {"type": "string"},
                            "data": {},
                            "timestamp": {"type": "integer"},
                        },
                    }
                }
            },
        }
    }


def op(summary, operation_id, parameters=None, request_body=None, tags=None):
    value = {
        "summary": summary,
        "operationId": operation_id,
        "tags": tags or ["数据分析与报告"],
        "responses": response_schema(summary),
    }
    if parameters:
        value["parameters"] = parameters
    if request_body:
        value["requestBody"] = request_body
    return value


def path_param(name):
    return {
        "name": name,
        "in": "path",
        "required": True,
        "schema": {"type": "integer"},
    }


def query_param(name, required=False):
    return {
        "name": name,
        "in": "query",
        "required": required,
        "schema": {"type": "string"},
    }


REPORT_SCHEMA = {
    "type": "object",
    "required": ["reportName", "dimensions", "metrics"],
    "properties": {
        "reportName": {"type": "string"},
        "dimensions": {"type": "array", "items": {"type": "string"}},
        "metrics": {"type": "array", "items": {"type": "string"}},
        "filters": {
            "type": "object",
            "additionalProperties": {"type": "string"},
        },
        "save": {"type": "boolean", "default": False},
    },
}


def build_openapi():
    report_body = {
        "required": True,
        "content": {"application/json": {"schema": REPORT_SCHEMA}},
    }
    return {
        "openapi": "3.0.3",
        "info": {
            "title": "智能企业人才梯队平台 - 数据分析与报告 API",
            "version": "1.0.0",
            "description": "人才盘点、流动趋势、自定义报表、模型效果与日志溯源接口。",
        },
        "servers": [
            {
                "url": "http://host.docker.internal:9090",
                "description": "Dify Docker 所在 Windows 主机",
            },
            {"url": "http://10.1.249.82:9090", "description": "当前局域网地址"},
            {"url": "http://26.37.52.184:9090", "description": "Radmin VPN 地址"},
            {"url": "http://127.0.0.1:9090", "description": "本机调试地址"},
        ],
        "paths": {
            "/api/analysis/dashboard": {
                "get": op("人才盘点看板", "getDashboard")
            },
            "/api/analysis/pipeline": {
                "get": op("人才梯队整体健康度", "getPipeline")
            },
            "/api/analysis/flow-trend": {
                "get": op("人才历史流动趋势", "getFlowTrend")
            },
            "/api/analysis/report/options": {
                "get": op("查询报表维度和指标", "getReportOptions")
            },
            "/api/analysis/report/generate": {
                "post": op("生成自定义报表", "generateReport", request_body=report_body)
            },
            "/api/analysis/report/from-template/{templateId}": {
                "post": op(
                    "按模板生成报表",
                    "generateReportFromTemplate",
                    parameters=[path_param("templateId")],
                )
            },
            "/api/analysis/report/history": {
                "get": op("查询历史报表", "getReportHistory")
            },
            "/api/analysis/report/templates": {
                "get": op("查询报表模板", "getReportTemplates"),
                "post": op("保存报表模板", "saveReportTemplate", request_body=report_body),
            },
            "/api/analysis/report/templates/{id}": {
                "delete": op(
                    "删除报表模板",
                    "deleteReportTemplate",
                    parameters=[path_param("id")],
                )
            },
            "/api/analysis/report/{id}": {
                "get": op("查询报表详情与溯源信息", "getReportDetail", parameters=[path_param("id")]),
                "delete": op("删除历史报表", "deleteReport", parameters=[path_param("id")]),
            },
            "/api/analysis/model/effect": {
                "get": op("查询模型运行效果", "getModelEffect")
            },
            "/api/analysis/model/logs": {
                "get": op(
                    "分页查询模型日志",
                    "getModelLogs",
                    parameters=[
                        query_param("modelName"),
                        query_param("status"),
                        query_param("version"),
                        query_param("pageNum"),
                        query_param("pageSize"),
                    ],
                )
            },
            "/api/analysis/model/logs/{id}": {
                "get": op("查询模型日志详情", "getModelLogDetail", parameters=[path_param("id")])
            },
            "/api/analysis/model/names": {
                "get": op("查询模型名称列表", "getModelNames")
            },
        },
    }


def main():
    with BASE_FILE.open("r", encoding="utf-8") as stream:
        source = yaml.safe_load(stream)
    workflow = build_workflow(source)
    with WORKFLOW_FILE.open("w", encoding="utf-8", newline="\n") as stream:
        yaml.safe_dump(
            workflow,
            stream,
            allow_unicode=True,
            sort_keys=False,
            width=1000,
        )
    with OPENAPI_FILE.open("w", encoding="utf-8", newline="\n") as stream:
        yaml.safe_dump(
            build_openapi(),
            stream,
            allow_unicode=True,
            sort_keys=False,
            width=1000,
        )
    print(WORKFLOW_FILE)
    print(OPENAPI_FILE)


if __name__ == "__main__":
    main()
