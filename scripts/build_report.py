# -*- coding: utf-8 -*-
"""生成《天津工业大学项目实训总结报告（晋升决策模块）》完整内容。"""
import os
from docx import Document
from docx.shared import Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.oxml.ns import qn

SRC = r"D:\Downloads\天津工业大学项目实训总结报告（大模型）.docx"
DST = os.environ.get("REPORT_OUT") or r"D:\202609shixun\shixun002\天津工业大学项目实训总结报告（晋升决策模块）.docx"

doc = Document(SRC)

# ---------- 1. 填充封面「项目名称」 ----------
for p in doc.paragraphs:
    if p.text.strip().startswith("项目名称"):
        run = p.add_run("智能企业人才梯队建设平台（晋升决策支持模块）")
        run.font.name = "楷体_GB2312"
        run._element.rPr.rFonts.set(qn("w:eastAsia"), "楷体_GB2312")
        run.font.size = Pt(18)
        break

# ---------- 2. 删除正文模板段落（从第 16 段「1 引言」开始到结尾） ----------
paras = doc.paragraphs
# 找到「1 引言」段索引
start = None
for i, p in enumerate(paras):
    if p.text.strip().startswith("1 引言"):
        start = i
        break
assert start is not None, "未找到正文起点"

body = doc.element.body
for p in paras[start:]:
    p._element.getparent().remove(p._element)

# ---------- 3. 排版辅助函数 ----------
def set_font(run, name, size, bold=False, color=None):
    run.font.name = name
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    run.font.size = Pt(size)
    run.font.bold = bold
    if color is not None:
        run.font.color.rgb = RGBColor(*color)

def add_h1(text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(10)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.first_line_indent = None
    set_font(p.add_run(text), "黑体", 15, bold=True)
    return p

def add_h2(text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.space_before = Pt(8)
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.first_line_indent = None
    p.paragraph_format.keep_with_next = True
    set_font(p.add_run(text), "黑体", 14, bold=False)
    return p

def add_h3(text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.first_line_indent = None
    set_font(p.add_run(text), "黑体", 12, bold=True)
    return p

def add_body(text, indent=True):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.line_spacing = 1.25
    pf.space_after = Pt(0)
    if indent:
        pf.first_line_indent = Pt(24)
    set_font(p.add_run(text), "宋体", 12)
    return p

def add_code(text):
    lines = text.strip("\n").split("\n")
    for ln in lines:
        p = doc.add_paragraph()
        pf = p.paragraph_format
        pf.line_spacing = 1.0
        pf.space_after = Pt(0)
        pf.first_line_indent = None
        pf.left_indent = Pt(12)
        set_font(p.add_run(ln if ln else " "), "宋体", 10.5)
    return

def add_ph(text):
    """截图/待替换占位：蓝色加粗，便于用户检索替换。"""
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.line_spacing = 1.25
    pf.space_after = Pt(0)
    pf.first_line_indent = Pt(24)
    set_font(p.add_run(text), "宋体", 12, bold=True, color=(0x00, 0x00, 0xFF))
    return p

def add_blank():
    doc.add_paragraph()

# ================= 正文 =================

add_h1("1 引言")

add_h2("1.1 项目概述")
add_body("项目名称：智能企业人才梯队建设平台（晋升决策支持模块）。")
add_body("本项目是面向企业人才管理的微服务平台，由天津市融创软通科技股份有限公司提供实训课题。平台围绕“人才梯队建设”这一核心场景，将整体业务拆分为员工档案智能管理、梯队规划与预测、智能培训推荐、晋升决策支持、数据分析与报告五大模块。各模块以微服务形式独立部署，通过 Nacos 服务注册与配置中心、Gateway 统一网关、Sentinel 限流熔断协同工作，形成一套可扩展的人才管理数字化底座。")
add_body("技术选型方面，后端采用 Java 17 + Spring Boot 3.2 + Spring Cloud Alibaba 2023 + MyBatis-Plus + MySQL 8，前端采用 Vue 3 + Vite + TypeScript + Element Plus，大模型能力通过 Dify 工作流平台接入。")
add_body("本报告围绕本人负责的“晋升决策支持”模块（服务名 talent-promotion-service，端口 8084）展开。该模块以数据驱动的规则引擎为核心，从 1000 名在职员工中为目标岗位自动筛选符合条件的晋升候选人，综合绩效、潜力、司龄、流失风险、继任准备度等多维证据进行量化排序，并集成 Dify 大模型工作流，提供自然语言交互的“晋升决策智能体”，辅助 HR 与管理者做出更客观、可解释的晋升决策。")
add_body("参考资料：企业提供的《员工人才梯队数据集（增强版，1000 人 21 字段）》《岗位画像表（72 岗位）》CSV 素材、项目《队友开发指南》及 Dify Chatflow 工作流文档。")

add_h2("1.2 需求分析")
add_body("在企业人才管理中，晋升决策长期依赖管理者主观判断，主要存在以下痛点：")
add_body("（1）标准不统一、不透明，候选人难以理解晋升依据，决策公平性易受质疑；")
add_body("（2）证据维度单一，往往只看绩效，忽视潜力、流失风险、继任准备度等关键信息，容易造成“高绩效但高流失风险”的员工被晋升、“低职级但高潜力”的员工被埋没；")
add_body("（3）候选池规模较大时，人工逐人比对效率低、易遗漏。")
add_body("针对上述问题，晋升决策模块需要实现：")
add_body("（1）规则化候选筛选：以“职级层级低于目标岗位 + 绩效达标”为硬条件，自动筛出候选人；")
add_body("（2）综合评分排序：按“绩效×15 + 潜力评分×0.5 + 司龄×2”计算综合分并降序排列；")
add_body("（3）风险与继任证据融合：关联流失预警记录标记风险等级，结合继任准备度（READY_NOW / READY_1_YEAR 等）与技能匹配度给出晋升建议；")
add_body("（4）AI 决策辅助：通过 Dify 大模型工作流，用自然语言完成“评估某岗位候选人”“比较前 5 名候选人准备度”等问答，输出可解释结论；")
add_body("（5）可视化呈现：提供候选人排行、绩效潜力矩阵、部门分布、风险关注、决策建议、晋升助手六个视图。")
add_body("实际价值：将晋升决策从“经验判断”升级为“规则 + 数据 + AI 辅助”的可追溯流程，提升决策的客观性、公平性与效率，降低错配风险，是企业人才梯队健康度的直接保障。")

add_h2("1.3 运行环境")
add_h3("（1）硬件环境")
add_body("处理器：Intel Core i5 及以上（或同等性能）；内存：16 GB 及以上；磁盘：可用空间 50 GB 以上。")
add_h3("（2）软件环境")
add_body("操作系统：Windows 11；JDK：17（编译与运行微服务）；Maven：3.6+；MySQL：8.0（数据库 talent_db）；Nacos Server：2.4.0.1（注册/配置中心，用 JDK 8 启动）；Redis：用于 Sentinel 数据存储（可选）；Node.js / pnpm：前端 talent-web 构建；Dify：大模型工作流平台（晋升决策智能体，可选）；浏览器：Chrome / Edge。")

add_h1("2 项目设计")

add_h2("2.1 设计思路")
add_body("晋升决策模块整体遵循“数据驱动、规则可配置、AI 增强、隐私优先”的设计思路：")
add_body("（1）分层架构。严格按 controller → service → mapper 分层：控制器只负责参数接收与统一返回，业务逻辑与校验下沉到 service，数据查询集中在 mapper，保证职责单一、易于维护。")
add_body("（2）规则在 SQL 层硬过滤。晋升硬条件（职级层级低于目标岗位、绩效 ≥ 4 分）直接在 mapper 的 SQL 中完成，利用 MySQL 的 FIELD() 函数把职级层级映射为可比较的序号，一次查询即完成筛选与排序，避免把大候选集拉到内存再过滤。")
add_body("（3）综合评分规则集中定义。评分公式（绩效×15 + 潜力×0.5 + 司龄×2）在 SQL 中计算并在 VO 中注释说明，权重调整只需改一处。")
add_body("（4）AI 助手采用“本地汇总证据 + 后端推送 Dify”的上下文注入式设计。晋升服务先通过本地网关聚合岗位、员工、绩效、技能、继任等数据，脱敏后生成 JSON 上下文，再作为 promotion_context 变量推送给 Dify 工作流；Dify 不回调本机接口，因此无需公网入口或内网隧道。")
add_body("（5）隐私与安全。上下文仅传递晋升决策所需字段，明确不传手机号、薪酬、年龄、性别等敏感信息；Dify API Key 只存于服务端配置，前端无法获取。")

add_h2("2.2 模块功能介绍")
add_body("晋升决策模块对外提供两类接口能力：")
add_body("（1）晋升候选人查询接口：GET /api/promotion/candidates?positionId=1&limit=10，入参为目标岗位 ID（positionId）与返回数量上限（limit，默认 10、最大 100），返回按综合分降序的候选人列表，字段包括工号、姓名、部门、职级层级、职级、司龄、绩效分、潜力等级、潜力分、流失风险等级、综合分等。")
add_body("（2）晋升决策 AI 助手：GET /api/promotion/ai/status 查询 Dify 是否已配置（configured / baseUrl），供前端提示；POST /api/promotion/ai/chat 接收 { query, conversationId, user }，本地汇总晋升证据后调用 Dify Chatflow，返回 answer、conversationId、messageId 等，支持多轮对话。")
add_body("对应前端页面（PromotionView.vue）提供六个标签页视图：候选人排行（按综合分排序的明细表）、绩效潜力矩阵（四象限分组）、部门分布（柱状图与层级分布）、风险关注（中高风险候选人及保留建议）、决策建议（综合分与行动建议）、晋升助手（决策智能体对话入口）。")

add_h2("2.3 模块结构图")
add_body("晋升决策模块按 controller → service → mapper 分层组织，包含候选人查询与 Dify 智能体两条业务链路，代码包结构如图 2-1 所示。")
add_ph("【结构图占位：如需要，可将上述包结构用 IDEA / draw.io 生成一张模块结构图插入此处，JPG 格式】")

add_h2("2.4 程序流程图")
add_body("晋升决策模块包含两条主流程：候选人查询流程与 AI 助手对话流程，如图 2-2 所示。")
add_ph("【流程图占位：如需要，可将上述两条流程用 Visio / draw.io 绘制成流程图插入此处，JPG 格式】")

add_h2("2.5 功能设计分工")
add_body("（1）团队分工（五大模块各由一名同学负责）：")
add_code("""模块 1  员工档案智能管理   talent-employee-service   端口 8081   同学 1
模块 2  梯队规划与预测       talent-planning-service    端口 8082   同学 2
模块 3  智能培训推荐         talent-training-service     端口 8083   同学 3
模块 4  晋升决策支持         talent-promotion-service    端口 8084   本人
模块 5  数据分析与报告       talent-analysis-service     端口 8085   同学 5
公共部分  talent-gateway（网关 9090）、talent-common（统一返回/异常/分页）由组长统一维护""")
add_body("（2）本人负责的晋升模块内部任务分工：候选人筛选与评分接口（controller + service + mapper + VO）；晋升决策 AI 助手（Dify 工作流代理 + 配置状态查询）；晋升证据汇总服务（跨模块数据聚合、脱敏、候选排序与推荐动作）；单元测试（岗位解析、候选排序规则）。")

add_h1("3 详细设计")

add_h2("3.1 系统设计")
add_body("本模块作为独立微服务，与外部系统的依赖关系如下：通过 MySQL 直连读取 emp_employee、position、emp_performance、emp_potential、resign_warning_record 等表；通过本地网关（http://127.0.0.1:9090）调用 /api/planning/positions、/api/planning/succession、/api/planning/skill-coverage、/api/employee/referenced、/api/employee/{id} 等接口聚合跨模块数据；通过 HTTPS 调用 Dify 的 /v1/chat-messages 完成大模型问答。接口统一使用 talent-common 提供的 Result 封装返回，异常由 GlobalExceptionHandler 统一转成标准 JSON。")

add_h2("3.2 数据库设计")
add_body("模块主要读写共享库 talent_db 中的以下表：")
add_body("（1）position（岗位画像）：position_name、department、level_tier（职级层级）、job_rank（职级）、perf_require（绩效要求）等；")
add_body("（2）emp_employee（员工信息）：emp_no、name、department、position_id、level_tier、job_rank、tenure_years、status 等；")
add_body("（3）emp_performance（绩效，按年度多条）：score、grade、perf_year；")
add_body("（4）emp_potential（潜力评估）：potential_level、total_score；")
add_body("（5）resign_warning_record（流失预警）：warning_level、risk_score；")
add_body("（6）pos_succession_candidate（继任候选）：match_score 等（供 AI 证据使用）；")
add_body("（7）pos_skill_require（岗位技能要求）：require_type（核心/加分）、skill_id（供 AI 技能覆盖使用）。")
add_body("晋升候选筛选的关联关系：以 emp_employee 为主表，join position 得到目标岗位层级，left join emp_performance（按 perf_year 过滤）、emp_potential、resign_warning_record 得到绩效、潜力与风险。")

add_h2("3.3 核心技术")
add_body("（1）职级层级比较：用 MySQL 的 FIELD() 把“员工层 < 主管层 < 经理层 < 总监层 < 高管层 < 决策层”映射为序号，要求目标岗位层级序号大于候选人层级序号，实现“只能晋升到更高层级”。")
add_body("（2）综合评分：round(ifnull(绩效,0)×15 + ifnull(潜力,0)×0.5 + 司龄×2, 1)，绩效占主导权重，兼顾潜力与稳定性。")
add_body("（3）上下文注入式 AI：PromotionEvidenceService 聚合证据并按规则评分排序，PromotionAssistantServiceImpl 通过 Spring RestClient 以 blocking 模式调用 Dify，并用正则清除 <think>…</think> 推理块，保证回复干净。")
add_body("（4）隐私脱敏：上下文生成时显式声明不传敏感字段，并对各来源数据做字段白名单清洗（sanitize* 系列方法）。")
add_body("（5）规则可配置：绩效年份、Dify 地址/Key、网关地址均通过 application.yml + 环境变量注入，无需改代码即可切换环境。")

add_h2("3.4 核心功能描述（晋升决策规则）")
add_body("（1）硬条件：在职 + 职级层级低于目标岗位 + 当年绩效 ≥ 4 分；")
add_body("（2）晋升综合分 promotionScore = clamp(totalScore / 150 × 100, 0, 100)；")
add_body("（3）完整证据决策分 decisionScore = 0.40×promotionScore + 0.35×继任匹配分 + 0.15×准备度分 + 0.10×风险分；仅有晋升证据（无继任数据）时 decisionScore = 0.80×promotionScore + 0.20×风险分；")
add_body("（4）准备度映射：READY_NOW=100、READY_1_YEAR=85、READY_2_YEARS=55、NOT_READY=20；风险映射：低风险=100、中风险=60、高风险=30；")
add_body("（5）排序：证据完整者优先；同组按准备度降序、再按决策分降序；")
add_body("（6）护栏规则：READY_2_YEARS、NOT_READY、核心技能匹配为 0 或技能分低于 40 者，不得直接推荐晋升。")

add_h1("4 实现方案")

add_h2("4.1 候选人筛选与评分实现")
add_body("核心 SQL 位于 PromotionMapper.selectCandidates，通过 FIELD() 比较层级并计算综合分：")
add_code("""@Select(\"\"\"
        select e.id as employee_id, e.emp_no, e.name, e.department,
               e.level_tier, e.job_rank, e.tenure_years,
               f.score as perf_score,
               po.potential_level, po.total_score as potential_score,
               w.warning_level,
               round(ifnull(f.score, 0) * 15 + ifnull(po.total_score, 0) * 0.5
                     + e.tenure_years * 2, 1) as total_score
        from emp_employee e
        join position target on target.id = #{positionId}
        left join emp_performance f on f.employee_id = e.id and f.perf_year = #{perfYear}
        left join emp_potential po on po.employee_id = e.id
        left join resign_warning_record w on w.employee_id = e.id
        where e.status = '在职'
          and field(target.level_tier, '员工层','主管层','经理层','总监层','高管层','决策层')
              > field(e.level_tier, '员工层','主管层','经理层','总监层','高管层','决策层')
          and ifnull(f.score, 0) >= 4
        order by total_score desc, po.total_score desc
        limit #{limit}
        \"\"\")""")

add_h2("4.2 业务校验实现")
add_body("PromotionServiceImpl.candidates 负责岗位存在性校验与 limit 边界处理：")
add_code("""public List<PromotionCandidateVO> candidates(Long positionId, int limit) {
    if (positionId == null || promotionMapper.countPosition(positionId) == 0) {
        throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "岗位不存在：" + positionId);
    }
    return promotionMapper.selectCandidates(positionId, perfYear,
            limit < 1 ? 10 : Math.min(limit, 100));
}""")

add_h2("4.3 晋升决策 AI 助手实现")
add_body("PromotionAssistantServiceImpl.chat 先由证据服务聚合上下文，再调用 Dify，并清理推理块：")
add_code("""String context = evidenceService.buildContext(dto.getQuery());
Map<String, Object> body = new LinkedHashMap<>();
body.put("inputs", Map.of("query", dto.getQuery(), "promotion_context", context));
body.put("query", dto.getQuery());
body.put("response_mode", "blocking");
body.put("conversation_id", dto.getConversationId() == null ? "" : dto.getConversationId());
body.put("user", StringUtils.hasText(dto.getUser()) ? dto.getUser() : defaultUser);

Map<String, Object> response = restClient().post().uri(endpoint())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() { });
vo.setAnswer(cleanAnswer(stringValue(response.get("answer"))));""")

add_h2("4.4 晋升证据汇总与排序实现")
add_body("PromotionEvidenceService.rankCandidates 将晋升候选池与继任池对齐，并按决策分排序、给出推荐动作：")
add_code("""double promotionScore = clamp(number(promotion.get("totalScore"), 0) / 150 * 100);
double riskScore = riskScore(text(promotion.get("warningLevel")));
if (succession == null) {
    candidate.put("evidenceCoverage", "promotion_only");
    candidate.put("decisionScore", round(clamp(0.80 * promotionScore + 0.20 * riskScore)));
    candidate.put("recommendedAction", "待补充继任评估，不直接下晋升结论");
} else {
    double decisionScore = 0.40 * promotionScore + 0.35 * successionMatch
            + 0.15 * readinessScore(readiness) + 0.10 * riskScore;
    candidate.put("evidenceCoverage", "full");
    candidate.put("decisionScore", round(clamp(decisionScore)));
    candidate.put("recommendedAction", recommendedAction(readiness, skillScore, matchedSkillCount));
}""")

add_h2("4.5 实现结果")
add_body("下面从系统运行界面开始，按操作步骤截图并说明（图片为 JPG 格式）。")
add_ph("【截图占位 1：候选人排行】浏览器打开前端晋升决策页面，在“选择要晋升去的目标岗位”下拉框选择岗位，点击“查询候选人”，截取“候选人排行”标签页，展示按综合分排序的候选人明细表。")
add_ph("【截图占位 2：绩效潜力矩阵】切换“绩效潜力矩阵”标签页，截取四象限分组结果。")
add_ph("【截图占位 3：部门分布】切换“部门分布”标签页，截取部门分布柱状图、层级分布与部门平均综合分。")
add_ph("【截图占位 4：风险关注】切换“风险关注”标签页，截取中高风险候选人列表及保留建议。")
add_ph("【截图占位 5：决策建议】切换“决策建议”标签页，截取综合分与行动建议列表。")
add_ph("【截图占位 6：晋升助手】切换“晋升助手”标签页，输入如“评估当前岗位的晋升候选人”并发送，截取智能体回复结果。")

add_h1("5 系统测试")

add_h2("5.1 测试方法")
add_body("（1）单元测试：采用 JUnit 5 + AssertJ，针对证据汇总服务的岗位解析与候选排序规则编写测试；")
add_body("（2）接口测试：使用 Postman / curl 分别直连服务（8084）与经网关（9090）访问接口；")
add_body("（3）集成测试：配合 Nacos、MySQL 及前端页面进行全链路验证。")

add_h2("5.2 测试描述")
add_body("测试用例 1（resolvePosition）：验证按完整岗位名（如“评估市场经理岗位”）与显式岗位 ID（如“评估岗位ID 8 的候选人”）均能正确解析出目标岗位。")
add_body("测试用例 2（rankCandidates）：验证 full 证据候选人优先、promotion_only 候选人与继任池分离，以及推荐动作判定正确——READY_NOW 且技能达标者返回“优先进入晋升评审”，READY_2_YEARS 者返回“进入继任池，暂不晋升”。")
add_body("边界与异常测试：岗位不存在返回 404；limit < 1 时取 10、limit > 100 时取 100；Dify 未配置时返回明确提示“请设置 PROMOTION_DIFY_BASE_URL 和 PROMOTION_DIFY_API_KEY”；候选池为空时前端显示“当前岗位暂无晋升候选人”。")
add_ph("【截图占位 7：单元测试结果】在 IDEA 中运行 PromotionEvidenceServiceTest，截取测试全部通过的界面。")
add_ph("【截图占位 8：接口测试结果】用 Postman 调用 GET /api/promotion/candidates?positionId=1&limit=5，截取返回 JSON 结果。")

add_h2("5.3 预期结果")
add_body("（1）候选筛选与排序符合规则：返回的候选人职级层级均低于目标岗位且绩效 ≥ 4 分，结果按综合分降序；")
add_body("（2）风险信息正确：中高风险候选人能在“风险关注”页被单独列出并给出保留建议；")
add_body("（3）AI 助手可用：配置 Dify 后，能返回与岗位/候选人相关的可解释结论；未配置时给出清晰的配置提示；")
add_body("（4）所有单元测试通过，接口异常场景返回统一错误码与提示信息。")

# ================= 保存 =================
doc.save(DST)
print("saved:", DST)
