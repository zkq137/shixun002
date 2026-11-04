# -*- coding: utf-8 -*-
"""生成「智能企业人才梯队建设平台」整项目最终汇报 PPT（12 页，16:9）。"""
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE, MSO_CONNECTOR
from pptx.enum.dml import MSO_LINE_DASH_STYLE

LOGO = r"D:\202609shixun\shixun002\school_logo.png"
ARCH = r"D:\202609shixun\shixun002\平台系统架构图.jpg"
OUT = r"D:\202609shixun\shixun002\智能企业人才梯队建设平台_最终汇报.pptx"

NAVY = RGBColor(0x1B, 0x2A, 0x4A)
BLUE = RGBColor(0x25, 0x63, 0xEB)
SKY = RGBColor(0xDB, 0xEA, 0xFE)
BG_SOFT = RGBColor(0xF4, 0xF7, 0xFB)
LINE = RGBColor(0xDF, 0xE6, 0xF0)
GRAY = RGBColor(0x33, 0x41, 0x55)
MUTED = RGBColor(0x6B, 0x7A, 0x8F)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
AMBER = RGBColor(0xB4, 0x53, 0x09)
AMBER_BG = RGBColor(0xFE, 0xF3, 0xC7)
GREEN = RGBColor(0x15, 0x80, 0x3D)
GREEN_BG = RGBColor(0xDC, 0xFC, 0xE7)
VIOLET_BG = RGBColor(0xEE, 0xF2, 0xFF)

FONT = "微软雅黑"

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
BLANK = prs.slide_layouts[6]


def add_text(slide, x, y, w, h, text, size=14, bold=False, color=GRAY,
             align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, line_spacing=1.15,
             margin=(0.02, 0.02, 0.02, 0.02)):
    tb = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    tf.margin_left = Inches(margin[0])
    tf.margin_right = Inches(margin[1])
    tf.margin_top = Inches(margin[2])
    tf.margin_bottom = Inches(margin[3])
    p = tf.paragraphs[0]
    p.alignment = align
    p.line_spacing = line_spacing
    r = p.add_run()
    r.text = text
    r.font.size = Pt(size)
    r.font.bold = bold
    r.font.color.rgb = color
    r.font.name = FONT
    return tb


def add_multiline(slide, x, y, w, h, lines, align=PP_ALIGN.LEFT,
                  anchor=MSO_ANCHOR.TOP, line_spacing=1.3):
    tb = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    tf.margin_left = Inches(0.02)
    tf.margin_right = Inches(0.02)
    tf.margin_top = Inches(0.02)
    tf.margin_bottom = Inches(0.02)
    first = True
    for (text, size, bold, color, before) in lines:
        p = tf.paragraphs[0] if first else tf.add_paragraph()
        first = False
        p.alignment = align
        p.line_spacing = line_spacing
        if before:
            p.space_before = Pt(before)
        r = p.add_run()
        r.text = text
        r.font.size = Pt(size)
        r.font.bold = bold
        r.font.color.rgb = color
        r.font.name = FONT
    return tb


def add_box(slide, x, y, w, h, fill=None, line=None, width=1.0,
            shape=MSO_SHAPE.ROUNDED_RECTANGLE, radius=None, dash=False):
    sp = slide.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    if radius is not None:
        try:
            sp.adjustments[0] = radius
        except Exception:
            pass
    if fill is None:
        sp.fill.background()
    else:
        sp.fill.solid()
        sp.fill.fore_color.rgb = fill
    if line is None:
        sp.line.fill.background()
    else:
        sp.line.color.rgb = line
        sp.line.width = Pt(width)
        if dash:
            sp.line.dash_style = MSO_LINE_DASH_STYLE.DASH
    sp.shadow.inherit = False
    sp.text_frame.text = ""
    return sp


def box_text(sp, text, size=14, bold=False, color=GRAY, align=PP_ALIGN.CENTER,
             anchor=MSO_ANCHOR.MIDDLE, line_spacing=1.1):
    tf = sp.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    tf.margin_left = Inches(0.06)
    tf.margin_right = Inches(0.06)
    tf.margin_top = Inches(0.02)
    tf.margin_bottom = Inches(0.02)
    p = tf.paragraphs[0]
    p.alignment = align
    p.line_spacing = line_spacing
    r = p.add_run()
    r.text = text
    r.font.size = Pt(size)
    r.font.bold = bold
    r.font.color.rgb = color
    r.font.name = FONT


def new_slide():
    return prs.slides.add_slide(BLANK)


def set_bg(slide, color):
    slide.background.fill.solid()
    slide.background.fill.fore_color.rgb = color


def content_header(slide, title, sub=None, page=None):
    add_box(slide, 0.55, 0.5, 0.08, 0.5, fill=BLUE, shape=MSO_SHAPE.RECTANGLE, radius=0)
    add_text(slide, 0.75, 0.4, 11.2, 0.66, title, size=26, bold=True, color=NAVY)
    if sub:
        add_text(slide, 0.78, 1.04, 11.6, 0.4, sub, size=12.5, color=MUTED)
    ln = slide.shapes.add_connector(MSO_CONNECTOR.STRAIGHT, Inches(0.55), Inches(6.98),
                                    Inches(12.78), Inches(6.98))
    ln.line.color.rgb = LINE
    ln.line.width = Pt(1)
    add_text(slide, 0.55, 7.05, 6.0, 0.28, "天津工业大学软件学院 · 智能企业人才梯队建设平台", size=9, color=MUTED)
    if page:
        add_text(slide, 11.9, 7.05, 0.9, 0.28, str(page), size=9, color=MUTED, align=PP_ALIGN.RIGHT)


def connector(slide, x1, y1, x2, y2, color=LINE, width=1.5, dash=False, arrow=False):
    ln = slide.shapes.add_connector(MSO_CONNECTOR.STRAIGHT, Inches(x1), Inches(y1), Inches(x2), Inches(y2))
    ln.line.color.rgb = color
    ln.line.width = Pt(width)
    if dash:
        ln.line.dash_style = MSO_LINE_DASH_STYLE.DASH
    if arrow:
        from pptx.oxml.ns import qn
        from lxml import etree
        lnEl = ln.line._get_or_add_ln()
        tail = lnEl.find(qn("a:tailEnd"))
        if tail is None:
            tail = etree.SubElement(lnEl, qn("a:tailEnd"))
        tail.set("type", "triangle")
        tail.set("w", "med")
        tail.set("len", "med")
    return ln


# ============ 1 封面 ============
s = new_slide()
set_bg(s, WHITE)
add_box(s, 0, 0, 13.333, 0.14, fill=NAVY, shape=MSO_SHAPE.RECTANGLE, radius=0)
s.shapes.add_picture(LOGO, Inches(5.97), Inches(0.5), Inches(1.4))
add_text(s, 0, 1.95, 13.333, 0.34, "TIANJIN  POLYTECHNIC  UNIVERSITY  ·  SCHOOL  OF  SOFTWARE",
         size=11, color=MUTED, align=PP_ALIGN.CENTER)
add_text(s, 0, 2.32, 13.333, 0.75, "智能企业人才梯队建设平台", size=40, bold=True, color=NAVY,
         align=PP_ALIGN.CENTER)
add_text(s, 0, 3.08, 13.333, 0.45, "五大模块 · 微服务架构 · 大模型智能体", size=19, color=BLUE,
         align=PP_ALIGN.CENTER)
add_box(s, 5.97, 3.72, 1.4, 0.035, fill=BLUE, shape=MSO_SHAPE.RECTANGLE, radius=0)
add_text(s, 0, 4.02, 13.333, 0.4, "实训总结汇报", size=18, bold=True, color=GRAY, align=PP_ALIGN.CENTER)
add_multiline(s, 2.45, 4.6, 8.45, 1.5, [
    ("汇报人：＿＿＿＿＿＿＿＿        专业班级：融创软通-大模型技术应用-23级工大班", 12, False, MUTED, 0),
    ("合作培养企业：天津市融创软通科技股份有限公司", 12, False, MUTED, 14),
    ("专业方向：大模型技术应用            实训时间：2026.8.31 — 2026.11.6", 12, False, MUTED, 14),
], align=PP_ALIGN.CENTER, line_spacing=1.3)
add_box(s, 0, 6.95, 13.333, 0.55, fill=NAVY, shape=MSO_SHAPE.RECTANGLE, radius=0)
add_text(s, 0, 7.08, 13.333, 0.3, "2026 年 11 月", size=11, color=RGBColor(0xD7, 0xE3, 0xF4), align=PP_ALIGN.CENTER)

# ============ 2 目录 ============
s = new_slide()
set_bg(s, WHITE)
content_header(s, "目 录", "CONTENTS", page=2)
items = [
    ("01", "团队与分工", "5 个模块 · 1 套公共底座"),
    ("02", "项目背景与目标", "为什么做 · 解决什么"),
    ("03", "主要功能列表", "五大模块功能树"),
    ("04", "系统架构与流程", "四层架构 · 请求链路"),
    ("05", "核心技术", "微服务 · 模型 · 智能体"),
    ("06", "项目特色", "统一智能体 · 可溯源"),
    ("07", "核心功能截图", "五大模块界面展示"),
    ("08", "总结与展望", "成果 · 收获 · 改进"),
]
for i, (no, title, desc) in enumerate(items):
    col = i % 2
    row = i // 2
    x = 0.75 + col * 6.15
    y = 1.72 + row * 1.22
    add_box(s, x, y, 5.7, 0.98, fill=BG_SOFT, line=LINE, width=0.75, radius=0.12)
    add_box(s, x + 0.18, y + 0.19, 0.6, 0.6, fill=BLUE, shape=MSO_SHAPE.RECTANGLE, radius=0.12)
    add_text(s, x + 0.18, y + 0.26, 0.6, 0.45, no, size=15, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
    add_text(s, x + 0.95, y + 0.14, 4.5, 0.4, title, size=17, bold=True, color=NAVY)
    add_text(s, x + 0.96, y + 0.57, 4.6, 0.3, desc, size=10.5, color=MUTED)

# ============ 3 团队与分工 ============
s = new_slide()
set_bg(s, WHITE)
content_header(s, "团队与分工", "5 人按模块独立开发，公共能力由组长统一维护", page=3)
rows = [
    ("序号", "负责模块", "部署服务 / 端口", "主要任务"),
    ("1", "员工档案智能管理", "talent-employee-service / 8081", "档案 CRUD、绩效潜力、技能树、Excel 导入导出"),
    ("2", "梯队规划与预测", "talent-planning-service / 8082", "岗位画像、继任候选、人才池、流失与岗位风险"),
    ("3", "智能培训推荐", "talent-training-service / 8083", "课程库、学习路径、培训记录、效果分析"),
    ("4", "晋升决策支持", "talent-promotion-service / 8084", "候选筛选、综合评分、风险提示、AI 决策助手"),
    ("5", "数据分析与报告", "talent-analysis-service / 8085", "梯队看板、流动预测、模型训练、报表溯源"),
    ("公共", "微服务基础设施", "talent-gateway / 9090", "网关路由、Nacos 注册配置、Sentinel 限流"),
]
tbl_shape = s.shapes.add_table(len(rows), 4, Inches(0.6), Inches(1.58), Inches(12.1), Inches(4.2))
table = tbl_shape.table
table.first_row = True
for i, w in enumerate([0.75, 2.45, 4.2, 4.7]):
    table.columns[i].width = Inches(w)
for r in range(len(rows)):
    table.rows[r].height = Inches(0.58 if r == 0 else 0.56)
    for c in range(4):
        cell = table.cell(r, c)
        cell.text = ""
        cell.margin_left = Inches(0.08)
        cell.margin_right = Inches(0.06)
        cell.margin_top = Inches(0.02)
        cell.margin_bottom = Inches(0.02)
        cell.vertical_anchor = MSO_ANCHOR.MIDDLE
        cell.fill.solid()
        cell.fill.fore_color.rgb = NAVY if r == 0 else (SKY if r == 6 else (WHITE if r % 2 else BG_SOFT))
        p = cell.text_frame.paragraphs[0]
        p.alignment = PP_ALIGN.CENTER if c == 0 else PP_ALIGN.LEFT
        run = p.add_run()
        run.text = rows[r][c]
        run.font.size = Pt(12.5 if r else 13)
        run.font.name = FONT
        run.font.bold = (r == 0)
        run.font.color.rgb = WHITE if r == 0 else GRAY

stats = [("1000", "名员工"), ("72", "个岗位"), ("172", "项技能"), ("58", "门课程")]
for i, (num, unit) in enumerate(stats):
    x = 0.62 + i * 3.05
    add_box(s, x, 6.0, 2.85, 0.78, fill=BG_SOFT, line=LINE, width=0.75, radius=0.12)
    add_text(s, x + 0.2, 6.08, 1.3, 0.6, num, size=20, bold=True, color=BLUE, anchor=MSO_ANCHOR.MIDDLE)
    add_text(s, x + 1.25, 6.08, 1.2, 0.6, unit, size=11.5, color=MUTED, anchor=MSO_ANCHOR.MIDDLE)

# ============ 4 项目背景与目标 ============
s = new_slide()
set_bg(s, WHITE)
content_header(s, "项目背景与目标", "把人才管理从「表格 + 经验」变成「可查询、可预测、可解释」", page=4)
add_text(s, 0.62, 1.48, 5.6, 0.35, "业务痛点", size=15, bold=True, color=BLUE)
pains = [
    ("数据分散", "档案、绩效、培训、岗位分散在多个表格，口径不一致"),
    ("梯队看不清", "关键岗位有没有接班人、缺口多大，靠人工盘点"),
    ("培养凭经验", "课程推什么、路径怎么排，缺少技能缺口的量化依据"),
    ("风险发现晚", "员工提出离职才发现流失风险，来不及干预"),
]
for i, (t, d) in enumerate(pains):
    y = 1.92 + i * 1.06
    add_box(s, 0.62, y, 5.55, 0.9, fill=WHITE, line=LINE, width=0.75, radius=0.1)
    add_box(s, 0.78, y + 0.22, 0.1, 0.44, fill=AMBER, shape=MSO_SHAPE.RECTANGLE, radius=0)
    add_text(s, 1.0, y + 0.1, 4.9, 0.35, t, size=14, bold=True, color=NAVY)
    add_text(s, 1.0, y + 0.46, 5.0, 0.35, d, size=10.5, color=MUTED)

add_text(s, 6.75, 1.48, 5.6, 0.35, "建设目标", size=15, bold=True, color=BLUE)
goals = [
    ("数据统一沉淀", "1000 员工、72 岗位统一入库，13 张表口径一致", SKY, RGBColor(0x1E, 0x46, 0xA8)),
    ("梯队可视化", "继任覆盖、人才池容量、风险预警一屏看清", GREEN_BG, RGBColor(0x14, 0x6A, 0x38)),
    ("培养与晋升有据", "技能缺口推荐课程，多维评分支持晋升决策", AMBER_BG, RGBColor(0x92, 0x40, 0x0E)),
    ("决策可解释", "规则与模型双轨，报表记录数据/模型版本", VIOLET_BG, RGBColor(0x1E, 0x3A, 0x8A)),
]
for i, (t, d, fill, fg) in enumerate(goals):
    y = 1.92 + i * 1.06
    add_box(s, 6.75, y, 5.6, 0.9, fill=fill, line=None, radius=0.1)
    add_text(s, 6.95, y + 0.1, 5.2, 0.35, t, size=14, bold=True, color=fg)
    add_text(s, 6.95, y + 0.46, 5.2, 0.35, d, size=10.5, color=GRAY)

add_box(s, 0.62, 6.28, 12.0, 0.55, fill=NAVY, radius=0.08)
add_text(s, 0.8, 6.37, 11.7, 0.36,
         "核心价值：让「谁该培养、谁有风险、谁可晋升」都能被数据回答，而不是被印象决定。",
         size=12, bold=True, color=WHITE)

# ============ 5 主要功能列表（树状结构） ============
s = new_slide()
set_bg(s, WHITE)
content_header(s, "主要功能列表（树状结构）", "平台拆为五大模块，每个模块都是「数据 + 分析 + AI 助手」的闭环", page=5)

root = add_box(s, 0.55, 3.0, 1.75, 1.05, fill=NAVY, radius=0.12)
box_text(root, "智能企业人才\n梯队建设平台", size=12.5, bold=True, color=WHITE)

mods = [
    ("员工档案智能管理", "员工名册 · 组织概览 · 绩效档案 · 潜力评估 · 技能树（9 类 / 172 项）"),
    ("梯队规划与预测", "岗位画像 · 继任候选 · 人才池容量 · 流失预警 · 关键岗位风险"),
    ("智能培训推荐", "课程库（58 门）· 学习路径与阶段任务 · 培训记录 · 培训效果"),
    ("晋升决策支持", "候选人筛选 · 综合评分 · 流失风险关注 · 晋升决策 AI 助手"),
    ("数据分析与报告", "梯队整体状态 · 人才流动趋势预测 · 模型效果与日志 · 自定义报表"),
]
mx, mw, mh = 2.9, 9.85, 0.84
tops = [1.72 + i * 0.94 for i in range(5)]
connector(s, 2.3, 3.52, 2.62, 3.52, color=LINE, width=1.5)
connector(s, 2.62, tops[0] + mh / 2, 2.62, tops[-1] + mh / 2, color=LINE, width=1.5)
for y0 in tops:
    connector(s, 2.62, y0 + mh / 2, mx, y0 + mh / 2, color=LINE, width=1.5)

for i, (name, funcs) in enumerate(mods):
    y = tops[i]
    add_box(s, mx, y, mw, mh, fill=BG_SOFT, line=LINE, width=0.9, radius=0.1)
    add_box(s, mx, y, 0.09, mh, fill=BLUE, shape=MSO_SHAPE.RECTANGLE, radius=0)
    add_text(s, mx + 0.24, y + 0.2, 2.35, 0.45, name, size=12.5, bold=True, color=NAVY,
             anchor=MSO_ANCHOR.MIDDLE)
    add_text(s, mx + 2.6, y + 0.13, 7.05, 0.6, funcs, size=9.5, color=MUTED,
             anchor=MSO_ANCHOR.MIDDLE, line_spacing=1.15)

add_box(s, 0.62, 6.5, 12.0, 0.42, fill=VIOLET_BG, radius=0.08)
add_text(s, 0.8, 6.56, 11.7, 0.3,
         "每个模块都配一个 Dify 智能助手（员工 / 规划 / 培训 / 晋升 / 分析），用自然语言完成查询与分析。",
         size=10.5, bold=True, color=RGBColor(0x1E, 0x3A, 0x8A))

# ============ 6 系统架构与流程 ============
s = new_slide()
set_bg(s, WHITE)
content_header(s, "系统架构与核心流程", "四层架构：前端 → 网关 → 五个微服务 → 数据与能力", page=6)

s.shapes.add_picture(ARCH, Inches(0.5), Inches(1.52), Inches(8.75))
add_box(s, 0.5, 1.52, 8.75, 4.56, fill=None, line=LINE, width=0.75, radius=0.02)

cx = 9.55
add_text(s, cx, 1.5, 3.3, 0.32, "一次请求的完整链路", size=13, bold=True, color=BLUE)
steps = [
    ("① 前端发起", "Vue3 + axios 封装，统一带上模块前缀"),
    ("② 网关路由", "按 /api/模块/** 转发，Sentinel 限流兜底"),
    ("③ 服务处理", "SQL 聚合 / 规则评分 / 模型推理 / 智能体调用"),
    ("④ 统一返回", "Result 封装，全局异常统一转换"),
    ("⑤ 前端渲染", "表格、矩阵、图表、助手对话"),
]
for i, (t, d) in enumerate(steps):
    y = 1.92 + i * 0.9
    add_box(s, cx, y, 3.25, 0.78, fill=BG_SOFT, line=LINE, width=0.75, radius=0.1)
    add_text(s, cx + 0.16, y + 0.1, 3.0, 0.3, t, size=11.5, bold=True, color=NAVY)
    add_text(s, cx + 0.16, y + 0.4, 3.0, 0.35, d, size=9, color=MUTED)

add_box(s, cx, 6.55, 3.25, 0.45, fill=AMBER_BG, radius=0.08)
add_text(s, cx + 0.14, 6.62, 3.0, 0.32, "跨模块数据：网关聚合 / Feign", size=9.5, bold=True, color=AMBER)

# ============ 7 核心技术 ============
s = new_slide()
set_bg(s, WHITE)
content_header(s, "项目核心技术", "微服务底座 + 数据与规则 + 机器学习 + 大模型智能体", page=7)
techs = [
    ("微服务与治理", "Spring Boot 3.2 + Spring Cloud Alibaba\nNacos 注册配置 / Gateway 路由 / Sentinel 限流\n5 个业务服务 + 1 个网关独立部署",
     "端口：8081—8085 · 网关 9090"),
    ("数据持久层", "MyBatis-Plus + MySQL 8，共享库 13 张表\n复杂查询用注解 SQL 多表关联\n分页插件统一接入，避免全表扫描",
     "表：绩效 / 潜力 / 技能 / 岗位关联查询"),
    ("机器学习模型", "标准化逻辑回归：流失风险 / 高潜识别 / 岗位匹配\n训练指标（AUC 等）写入模型日志\n模型参数持久化，支持在线推理",
     "specOf()：ATTRITION · POTENTIAL · POSITION_FIT"),
    ("大模型智能体", "Dify 工作流 × 5，blocking 模式 + 多轮对话\n后端汇总业务数据后注入上下文\nDify 不回调内网，无需公网隧道",
     "接口：POST /v1/chat-messages"),
    ("Excel 导入导出", "EasyExcel 模板下载 / 批量导入 / 结果回执\n导入失败行带原因回传\n导出走流式写出，避免内存溢出",
     "父工程锁定 commons-io 2.16.1"),
    ("前端可视化", "Vue 3 + TypeScript + Element Plus\n表格 / 矩阵 / 柱状图 / 进度条多形态\n统一请求封装与按标签页懒加载",
     "5 个模块页面 + 技能体系页"),
]
for i, (t, d, note) in enumerate(techs):
    c, r = i % 3, i // 3
    x = 0.62 + c * 4.13
    y = 1.62 + r * 2.5
    add_box(s, x, y, 3.9, 2.2, fill=WHITE, line=LINE, width=0.75, radius=0.08)
    add_box(s, x, y, 3.9, 0.08, fill=BLUE, shape=MSO_SHAPE.RECTANGLE, radius=0)
    add_text(s, x + 0.22, y + 0.28, 3.4, 0.4, t, size=15, bold=True, color=NAVY)
    add_text(s, x + 0.22, y + 0.82, 3.5, 1.02, d, size=10, color=MUTED, line_spacing=1.35)
    add_text(s, x + 0.22, y + 1.8, 3.5, 0.3, note, size=8.5, color=BLUE)

# ============ 8 项目特色 ============
s = new_slide()
set_bg(s, WHITE)
content_header(s, "项目特色", "技术特色与业务特色的结合点", page=8)
feats = [
    ("01  全模块统一的 AI 助手", "五个模块各配一个 Dify 智能体，共用一套「证据汇总 → 上下文注入 → 对话」范式，\n交互一致、密钥只在服务端。",
     SKY, "对应实现：各模块 *AssistantServiceImpl + AssistantChatDTO"),
    ("02  规则 + 模型双轨可解释", "晋升用加权规则给分，分析模块用逻辑回归给概率；报表同时记录数据版本与\n模型版本，能回答「这个数字怎么来的」。",
     GREEN_BG, "对应实现：PromotionMapper / TalentModelServiceImpl / ReportServiceImpl"),
    ("03  跨模块数据打通", "岗位、员工、继任、技能、培训在服务间流动：晋升读取岗位与继任数据，\n培训按技能缺口推荐课程。",
     AMBER_BG, "对应实现：网关聚合 / EmployeeClient(Feign)"),
    ("04  隐私与降级设计", "只传决策必需字段，手机号、薪酬、年龄、性别不出库；智能体未配置时\n给出明确提示，不阻断主流程。",
     VIOLET_BG, "对应实现：sanitize* 脱敏 + 环境变量配置"),
]
for i, (t, d, fill, note) in enumerate(feats):
    c, r = i % 2, i // 2
    x = 0.62 + c * 6.15
    y = 1.62 + r * 2.5
    add_box(s, x, y, 5.9, 2.2, fill=fill, line=None, radius=0.08)
    add_text(s, x + 0.28, y + 0.24, 5.3, 0.42, t, size=15, bold=True, color=NAVY)
    add_text(s, x + 0.28, y + 0.76, 5.35, 0.95, d, size=10, color=GRAY, line_spacing=1.35)
    add_text(s, x + 0.28, y + 1.82, 5.35, 0.3, note, size=8.5, color=MUTED)

# ============ 9-10 核心功能截图 ============
shots = [
    [("员工档案 · 员工名册", "档案检索、批量导入导出、技能与绩效一览"),
     ("梯队规划 · 继任候选", "关键岗位继任匹配度与准备度"),
     ("智能培训 · 推荐计划", "按技能缺口推荐课程与学习路径")],
    [("晋升决策 · 候选人排行", "综合分排序，含绩效、潜力、流失风险"),
     ("数据分析 · 梯队看板", "梯队健康度、人才池容量与流动趋势"),
     ("智能助手", "各模块 Dify 智能体：自然语言查询与分析")],
]
for si, group in enumerate(shots):
    s = new_slide()
    set_bg(s, WHITE)
    content_header(s, f"核心功能截图（{'一二'[si]}）", "仅作功能展示，讲解时略过", page=9 + si)
    for i, (name, desc) in enumerate(group):
        x = 0.62 + i * 4.2
        add_box(s, x, 1.72, 3.85, 3.05, fill=BG_SOFT, line=MUTED, width=1.2, radius=0.05, dash=True)
        add_text(s, x, 2.95, 3.85, 0.4, "在此插入截图", size=13, bold=True, color=MUTED, align=PP_ALIGN.CENTER)
        add_text(s, x, 3.35, 3.85, 0.35, name, size=11, color=BLUE, align=PP_ALIGN.CENTER)
        add_text(s, x, 4.9, 3.85, 0.4, f"图 {9 + si}-{i + 1}  {name}", size=10.5, color=MUTED,
                 align=PP_ALIGN.CENTER)
        add_text(s, x, 5.3, 3.85, 0.7, desc, size=9.5, color=MUTED, align=PP_ALIGN.CENTER, line_spacing=1.2)
    add_text(s, 0.62, 6.35, 12.0, 0.35,
             "提示：插入截图后删除虚线框与「在此插入截图」文字，保留图注即可；建议截图宽高比 16:9。",
             size=9.5, color=MUTED)

# ============ 11 总结与展望 ============
s = new_slide()
set_bg(s, WHITE)
content_header(s, "总结与展望", "完成了什么 · 收获了什么 · 还能怎么改", page=11)
cols = [
    ("完成情况", [
        "五大模块 + 网关全部上线，1000 名员工数据贯通",
        "5 个微服务独立部署，统一网关接入与限流",
        "3 个机器学习模型可训练、可部署、可在线推理",
        "5 个 Dify 智能体覆盖各业务模块",
        "报表支持数据版本与模型版本溯源",
    ], BLUE),
    ("能力收获", [
        "掌握微服务拆分、联调与网关治理",
        "学会把业务规则与指标落到 SQL、模型里",
        "理解大模型应用的工程化：注入、脱敏、降级",
        "学会用图表与流程把复杂方案讲清楚",
        "习惯用可解释的指标替代主观判断",
    ], GREEN),
    ("不足与展望", [
        "模型特征可纳入更多行为数据，提升预测精度",
        "权限与字段级脱敏需按角色进一步细化",
        "移动端与消息推送能力待补充",
        "报表模板可支持更多维度自由组合",
    ], AMBER),
]
for i, (title, items, accent) in enumerate(cols):
    x = 0.62 + i * 4.15
    add_box(s, x, 1.62, 3.92, 3.95, fill=WHITE, line=LINE, width=0.75, radius=0.07)
    add_box(s, x, 1.62, 3.92, 0.09, fill=accent, shape=MSO_SHAPE.RECTANGLE, radius=0)
    add_text(s, x + 0.25, 1.85, 3.4, 0.38, title, size=15, bold=True, color=NAVY)
    lines = [("· " + it, 10.5, False, GRAY, 14) for it in items]
    add_multiline(s, x + 0.22, 2.4, 3.5, 3.0, lines, line_spacing=1.45)

add_box(s, 0.62, 5.78, 12.0, 0.62, fill=BG_SOFT, line=LINE, width=0.75, radius=0.08)
add_text(s, 0.85, 5.9, 11.6, 0.4,
         "下一步：补权限体系与数据治理，把「能看、能算、能问」继续做成「能预警、能闭环」。",
         size=11.5, bold=True, color=NAVY)

# ============ 12 封底 ============
s = new_slide()
set_bg(s, NAVY)
add_box(s, 5.85, 0.9, 1.65, 1.55, fill=WHITE, shape=MSO_SHAPE.ROUNDED_RECTANGLE, radius=0.08)
s.shapes.add_picture(LOGO, Inches(6.02), Inches(1.02), Inches(1.3))
add_text(s, 0, 2.95, 13.333, 0.85, "感 谢 聆 听", size=44, bold=True, color=WHITE, align=PP_ALIGN.CENTER)
add_box(s, 6.27, 3.95, 0.8, 0.035, fill=RGBColor(0x7D, 0xA7, 0xE8), shape=MSO_SHAPE.RECTANGLE, radius=0)
add_text(s, 0, 4.2, 13.333, 0.45, "请各位老师批评指正", size=19, color=RGBColor(0xC7, 0xD6, 0xEE),
         align=PP_ALIGN.CENTER)
add_text(s, 0, 6.55, 13.333, 0.35,
         "天津工业大学软件学院 · 融创软通-大模型技术应用-23级工大班 · 2026.11",
         size=10.5, color=RGBColor(0x9F, 0xB4, 0xD4), align=PP_ALIGN.CENTER)

prs.core_properties.title = "智能企业人才梯队建设平台 · 实训总结汇报"
prs.core_properties.author = "天津工业大学软件学院"
prs.save(OUT)
print("saved:", OUT, "slides:", len(prs.slides))
