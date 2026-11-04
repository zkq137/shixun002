# -*- coding: utf-8 -*-
"""生成《智能企业人才梯队建设平台》最终汇报 PPT（16:9，15 页）。"""
import os
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR, MSO_AUTO_SIZE
from pptx.enum.shapes import MSO_SHAPE, MSO_CONNECTOR
from pptx.oxml.ns import qn
from lxml import etree

ROOT = r"D:\202609shixun\shixun002"
LOGO = os.path.join(ROOT, ".tmp", "school_logo_t.png")
OUT = os.path.join(ROOT, "智能企业人才梯队建设平台_最终报告.pptx")
SHOTS = os.path.join(ROOT, ".tmp", "shots")

# ---------------- palette ----------------
INK = RGBColor(0x0F, 0x17, 0x2A)
INK2 = RGBColor(0x1E, 0x29, 0x3B)
PURPLE = RGBColor(0x5B, 0x21, 0xB6)
PURPLE_MID = RGBColor(0x7C, 0x3A, 0xED)
PURPLE_LT = RGBColor(0xA7, 0x8B, 0xFA)
PURPLE_PALE = RGBColor(0xF3, 0xE8, 0xFF)
LAV = RGBColor(0xED, 0xE9, 0xFE)
INDIGO = RGBColor(0x43, 0x38, 0xCA)
CYAN = RGBColor(0x0E, 0x74, 0x90)
CYAN_PALE = RGBColor(0xEC, 0xFE, 0xFF)
GREEN = RGBColor(0x04, 0x78, 0x57)
GREEN_PALE = RGBColor(0xD1, 0xFA, 0xE5)
AMBER = RGBColor(0xB4, 0x53, 0x09)
AMBER_PALE = RGBColor(0xFE, 0xF3, 0xC7)
RED = RGBColor(0xB9, 0x1C, 0x1C)
GRAY = RGBColor(0x33, 0x41, 0x55)
MUTED = RGBColor(0x64, 0x74, 0x8B)
FAINT = RGBColor(0x94, 0xA3, 0xB8)
LINE = RGBColor(0xE5, 0xE7, 0xEF)
BG = RGBColor(0xF7, 0xF7, 0xFB)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
DARK_BG = RGBColor(0x0A, 0x12, 0x22)
DARK_LINE = RGBColor(0x2A, 0x35, 0x4D)

CN = "Microsoft YaHei"
NUM = "Bahnschrift"
W, H = 13.333, 7.5

prs = Presentation()
prs.slide_width = Inches(W)
prs.slide_height = Inches(H)
BLANK = prs.slide_layouts[6]

TOTAL = 15
page = 0


# ---------------- low level helpers ----------------
def _set_font(run, font, size, bold, color, italic=False, spc=0):
    run.font.name = font
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    run.font.color.rgb = color
    rPr = run._r.get_or_add_rPr()
    ea = rPr.find(qn("a:ea"))
    if ea is None:
        ea = etree.SubElement(rPr, qn("a:ea"))
    ea.set("typeface", font)
    if spc:
        rPr.set("spc", str(int(spc * 100)))


def text(slide, x, y, w, h, s, size=12, bold=False, color=INK, align=PP_ALIGN.LEFT,
         anchor=MSO_ANCHOR.TOP, font=CN, line=1.15, spc=0, italic=False):
    box = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.word_wrap = True
    tf.auto_size = MSO_AUTO_SIZE.NONE
    tf.vertical_anchor = anchor
    for attr in ("margin_left", "margin_right", "margin_top", "margin_bottom"):
        setattr(tf, attr, 0)
    p = tf.paragraphs[0]
    p.alignment = align
    p.line_spacing = line
    r = p.add_run()
    r.text = s
    _set_font(r, font, size, bold, color, italic, spc)
    return box


def para(tf, s, size=12, bold=False, color=INK, font=CN, line=1.3, space_before=0,
         space_after=0, align=PP_ALIGN.LEFT, spc=0, italic=False):
    p = tf.add_paragraph()
    p.alignment = align
    p.line_spacing = line
    p.space_before = Pt(space_before)
    p.space_after = Pt(space_after)
    r = p.add_run()
    r.text = s
    _set_font(r, font, size, bold, color, italic, spc)
    return p


def rich_para(tf, parts, size=10, color=GRAY, line=1.3, space_after=4, first=False):
    """parts: list of (text, bold, color, font, size_override)"""
    p = tf.paragraphs[0] if first else tf.add_paragraph()
    p.line_spacing = line
    p.space_after = Pt(space_after)
    for t, b, c, f, sz in parts:
        r = p.add_run()
        r.text = t
        _set_font(r, f or CN, sz or size, b, c or color)
    return p


def _alpha(fill_or_line, pct):
    try:
        srgb = fill_or_line.fore_color._xFill.find(qn("a:srgbClr"))
    except Exception:
        return
    if srgb is None:
        return
    a = etree.SubElement(srgb, qn("a:alpha"))
    a.set("val", str(int(pct * 100000)))


def rect(slide, x, y, w, h, fill=None, line=None, lw=1, radius=0.08, dash=None,
         shape=MSO_SHAPE.ROUNDED_RECTANGLE, alpha=None, shadow=False):
    sh = slide.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    if shape == MSO_SHAPE.ROUNDED_RECTANGLE and radius is not None:
        sh.adjustments[0] = radius
    if fill is None:
        sh.fill.background()
    else:
        sh.fill.solid()
        sh.fill.fore_color.rgb = fill
        if alpha is not None:
            _alpha(sh.fill, alpha)
    if line is None:
        sh.line.fill.background()
    else:
        sh.line.color.rgb = line
        sh.line.width = Pt(lw)
        if dash:
            sh.line.dash_style = dash
    sh.shadow.inherit = False
    if shadow:
        soft_shadow(sh)
    return sh


def soft_shadow(shape, blur=14, dist=3, alpha=0.10):
    spPr = shape._element.spPr
    effect = spPr.find(qn("a:effectLst"))
    if effect is not None:
        spPr.remove(effect)
    effect = etree.SubElement(spPr, qn("a:effectLst"))
    shd = etree.SubElement(effect, qn("a:outerShdw"))
    shd.set("blurRad", str(int(blur * 12700)))
    shd.set("dist", str(int(dist * 12700)))
    shd.set("dir", "5400000")
    shd.set("algn", "ctr")
    shd.set("rotWithShape", "0")
    clr = etree.SubElement(shd, qn("a:srgbClr"))
    clr.set("val", "1E293B")
    a = etree.SubElement(clr, qn("a:alpha"))
    a.set("val", str(int(alpha * 100000)))


def line(slide, x1, y1, x2, y2, color=LINE, lw=1, arrow=False, dash=None):
    conn = slide.shapes.add_connector(MSO_CONNECTOR.STRAIGHT, Inches(x1), Inches(y1),
                                      Inches(x2), Inches(y2))
    conn.line.color.rgb = color
    conn.line.width = Pt(lw)
    if dash:
        conn.line.dash_style = dash
    if arrow:
        ln = conn._element.spPr.get_or_add_ln()
        tail = etree.SubElement(ln, qn("a:tailEnd"))
        tail.set("type", "triangle")
        tail.set("w", "med")
        tail.set("len", "med")
    return conn


def circle_num(slide, x, y, d, n, fill=PURPLE, color=WHITE, size=10, font=NUM):
    sh = slide.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x), Inches(y), Inches(d), Inches(d))
    sh.fill.solid()
    sh.fill.fore_color.rgb = fill
    sh.line.fill.background()
    sh.shadow.inherit = False
    tf = sh.text_frame
    tf.word_wrap = False
    tf.auto_size = MSO_AUTO_SIZE.NONE
    tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    for attr in ("margin_left", "margin_right", "margin_top", "margin_bottom"):
        setattr(tf, attr, 0)
    p = tf.paragraphs[0]
    p.alignment = PP_ALIGN.CENTER
    r = p.add_run()
    r.text = str(n)
    _set_font(r, font, size, True, color)
    return sh


def logo(slide, x, y, d, card=True, card_fill=WHITE):
    if card:
        c = slide.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x - 0.06), Inches(y - 0.06),
                                   Inches(d + 0.12), Inches(d + 0.12))
        c.fill.solid()
        c.fill.fore_color.rgb = card_fill
        c.line.color.rgb = RGBColor(0xE9, 0xD8, 0xF5)
        c.line.width = Pt(1)
        c.shadow.inherit = False
    slide.shapes.add_picture(LOGO, Inches(x), Inches(y), Inches(d), Inches(d))


# ---------------- page furniture ----------------
def header(num, label, title, sub=None):
    global page
    page += 1
    s = prs.slides.add_slide(BLANK)
    bg = s.background.fill
    bg.solid()
    bg.fore_color.rgb = WHITE
    # subtle top-left wash
    wash = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(W), Inches(0.06))
    wash.fill.solid()
    wash.fill.fore_color.rgb = PURPLE
    wash.line.fill.background()
    wash.shadow.inherit = False
    # section number
    text(s, 0.7, 0.46, 0.55, 0.3, num, 11, True, PURPLE, font=NUM, spc=1)
    line(s, 0.72, 0.78, 1.06, 0.78, PURPLE_LT, 2)
    text(s, 1.1, 0.46, 5.5, 0.3, label, 9.5, True, PURPLE, spc=2.2)
    text(s, 0.68, 0.76, 11.2, 0.55, title, 24, True, INK)
    if sub:
        text(s, 0.7, 1.33, 11.4, 0.34, sub, 10.5, False, MUTED)
    line(s, 0.7, 1.68, 12.63, 1.68, LINE, 1)
    logo(s, 12.05, 0.42, 0.44)
    footer(s)
    return s


def footer(s, dark=False):
    c1 = RGBColor(0x6B, 0x72, 0x88) if not dark else RGBColor(0x7C, 0x8D, 0xA6)
    line(s, 0.7, 7.1, 12.63, 7.1, RGBColor(0xEE, 0xEF, 0xF5) if not dark else RGBColor(0x25, 0x30, 0x45), 0.75)
    text(s, 0.7, 7.17, 7.5, 0.22, "天津工业大学软件学院 · 智能企业人才梯队建设平台", 7.5, False, c1)
    text(s, 11.55, 7.16, 0.65, 0.24, f"{page:02d}", 10, True, PURPLE if not dark else PURPLE_LT, align=PP_ALIGN.RIGHT, font=NUM)
    text(s, 12.23, 7.18, 0.4, 0.22, f"/ {TOTAL}", 7.5, False, c1, align=PP_ALIGN.RIGHT, font=NUM)


def pic_card(s, img, x, y, w, h, caption=None, tag=None):
    # soft shadow card + white border + picture
    back = rect(s, x - 0.03, y - 0.03, w + 0.06, h + 0.06, WHITE, RGBColor(0xDD, 0xE2, 0xEC), 0.9, radius=0.03, shadow=True)
    s.shapes.add_picture(os.path.join(SHOTS, img), Inches(x), Inches(y), Inches(w), Inches(h))
    if tag:
        t = rect(s, x + 0.14, y + 0.14, 0.62, 0.24, INK, None, radius=0.3, alpha=85)
        text(s, x + 0.14, y + 0.17, 0.62, 0.18, tag, 7, True, WHITE, align=PP_ALIGN.CENTER)
    if caption:
        text(s, x, y + h + 0.14, w, 0.3, caption, 8.8, False, MUTED, align=PP_ALIGN.CENTER)


def notes(s, t):
    s.notes_slide.notes_text_frame.text = t


# =====================================================================
# 01 cover
# =====================================================================
page = 1
s = prs.slides.add_slide(BLANK)
bg = s.background.fill
bg.solid()
bg.fore_color.rgb = DARK_BG
# decorative rings
for (x, y, d, col, wd) in [(-1.2, -2.0, 7.4, RGBColor(0x22, 0x1B, 0x3A), 1),
                           (8.2, -2.6, 8.6, RGBColor(0x2A, 0x1F, 0x4A), 1),
                           (10.4, 1.9, 4.6, RGBColor(0x33, 0x25, 0x5C), 0.9)]:
    r = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x), Inches(y), Inches(d), Inches(d))
    r.fill.background()
    r.line.color.rgb = col
    r.line.width = Pt(wd)
    r.shadow.inherit = False
# glow

logo(s, 0.72, 0.55, 0.82)
text(s, 1.72, 0.62, 3.4, 0.3, "天津工业大学 · 软件学院", 10, True, RGBColor(0xE2, 0xE8, 0xF0), spc=1.2)
text(s, 1.72, 0.92, 3.6, 0.26, "TIANGONG UNIVERSITY · SCHOOL OF SOFTWARE", 6.8, False, RGBColor(0x7C, 0x8D, 0xA6), font=NUM, spc=1.1)
text(s, 9.2, 0.66, 3.4, 0.26, "项目实训 · 2026", 8.5, True, RGBColor(0x7C, 0x8D, 0xA6), align=PP_ALIGN.RIGHT, spc=1.6)

line(s, 0.75, 1.86, 1.15, 1.86, PURPLE_LT, 2.5)
text(s, 1.3, 1.76, 4.0, 0.3, "项 目 实 训 总 结 汇 报", 9.5, True, RGBColor(0xC4, 0xB5, 0xFD), spc=3)

text(s, 0.72, 2.18, 8.4, 0.82, "智能企业人才梯队", 42, True, WHITE)
text(s, 0.72, 2.98, 8.4, 0.82, "建设平台", 42, True, WHITE)

text(s, 0.75, 3.96, 8.0, 0.36, "五大微服务模块 · 可训练人才模型 · 大模型智能体", 15, False, RGBColor(0xA9, 0xB6, 0xC9))
line(s, 0.75, 4.55, 4.55, 4.55, RGBColor(0x3A, 0x2B, 0x60), 1)
text(s, 0.75, 4.72, 7.8, 0.3, "汇报人：牛亚晨　王浩阳　李秉谦　朱坤奇　班火明", 10.5, False, RGBColor(0xD5, 0xDD, 0xE8))
text(s, 0.75, 5.1, 7.9, 0.28, "专业方向：大模型技术应用　　专业班级：融创软通-大模型技术应用-23级工大班", 8.5, False, RGBColor(0x7C, 0x8D, 0xA6))
text(s, 0.75, 6.62, 3.0, 0.28, "2026 年 11 月", 9, False, RGBColor(0x6B, 0x7F, 0xA0), font=NUM, spc=1)

# right module rail
text(s, 9.05, 1.74, 3.6, 0.26, "五 大 业 务 模 块", 7.5, True, RGBColor(0x6B, 0x7F, 0xA0), spc=3)
line(s, 9.18, 2.12, 9.18, 5.62, RGBColor(0x2E, 0x3A, 0x55), 1)
mods = [("员工档案智能管理", "8081", "档案·技能"), ("梯队规划与预测", "8082", "岗位·继任"),
        ("智能培训推荐", "8083", "课程·路径"), ("晋升决策支持", "8084", "评分·建议"),
        ("数据分析与报告", "8085", "看板·模型")]
for i, (nm, port, kw) in enumerate(mods):
    y = 2.18 + i * 0.7
    d = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(9.135), Inches(y + 0.09), Inches(0.09), Inches(0.09))
    d.fill.solid(); d.fill.fore_color.rgb = PURPLE_LT; d.line.fill.background(); d.shadow.inherit = False
    text(s, 9.4, y, 2.5, 0.28, nm, 10.5, False, WHITE)
    text(s, 9.4, y + 0.29, 1.9, 0.22, kw, 7.5, False, RGBColor(0x6B, 0x7F, 0xA0))
    text(s, 11.95, y + 0.02, 0.65, 0.24, port, 7.5, False, RGBColor(0x8B, 0x9B, 0xB5), align=PP_ALIGN.RIGHT, font=NUM)
notes(s, "开场：本项目是天津工业大学软件学院实训成果，5 人小组用微服务 + 大模型做了一个智能企业人才梯队建设平台。今天按大纲讲：团队、目标、功能、流程、技术、特色、截图、总结。")

# =====================================================================
# 02 contents
# =====================================================================
page = 2
s = prs.slides.add_slide(BLANK)
bg = s.background.fill; bg.solid(); bg.fore_color.rgb = WHITE
logo(s, 12.05, 0.42, 0.44)
text(s, 0.72, 1.15, 3.4, 0.85, "目 录", 40, True, INK)
text(s, 0.75, 1.98, 3.4, 0.28, "C O N T E N T S", 9, True, PURPLE, font=NUM, spc=3.4)
line(s, 0.75, 2.42, 3.05, 2.42, PURPLE_LT, 2)
text(s, 0.75, 2.62, 3.15, 0.95, "从团队与目标出发，依次介绍功能、流程、技术、特色，最后用真实截图与总结收尾。", 9.5, False, MUTED, line=1.55)

# stats card bottom-left
c = rect(s, 0.72, 5.62, 3.05, 1.08, BG, None, radius=0.06)
text(s, 0.92, 5.76, 2.7, 0.24, "5 大模块 · 6 个服务 · 1000 名员工", 8.5, True, PURPLE)
text(s, 0.92, 6.06, 2.7, 0.5, "29 张表 · 172 项技能 · 58 门课程\n五个模块共用一套数据与字典", 7.5, False, MUTED, line=1.5)

items = [("01", "团队与分工", "TEAM & DIVISION"), ("02", "项目目的与意义", "OBJECTIVE & VALUE"),
         ("03", "主要功能列表", "FUNCTION MAP"), ("04", "核心业务流程", "CORE WORKFLOW"),
         ("05", "项目核心技术", "TECHNOLOGY"), ("06", "项目特色", "HIGHLIGHTS"),
         ("07", "核心功能截图", "SCREENSHOTS"), ("08", "总结与展望", "SUMMARY")]
for i, (n, t, e) in enumerate(items):
    col = i // 4
    row = i % 4
    x = 4.55 + col * 4.12
    y = 1.16 + row * 1.36
    text(s, x, y + 0.06, 0.6, 0.3, n, 11, True, PURPLE, font=NUM, spc=1)
    text(s, x + 0.56, y, 3.3, 0.42, t, 16.5, True, INK)
    text(s, x + 0.58, y + 0.44, 3.2, 0.24, e, 7, False, FAINT, font=NUM, spc=1.6)
    line(s, x, y + 0.96, x + 3.6, y + 0.96, LINE, 0.9)
text(s, 4.55, 6.62, 8.0, 0.3, "封面 / 目录不计入章节编号，正文共 8 个部分。", 8, False, FAINT, italic=True)
footer(s)
notes(s, "目录页快速过：八个部分对应任务书大纲，封面、目录、封底不占章节号。")

# =====================================================================
# 03 team
# =====================================================================
s = header("01", "T E A M", "5 人小组 · 一人一模块 · 公共层统一维护",
           "每人独立负责一个微服务模块，通过网关与 Feign 协作；公共能力由架构负责人统一沉淀")
MEMBERS = [
    ("牛", "牛亚晨", "晋升决策支持", "8084", "评分 · 建议",
     ["跨层级候选筛选与综合评分", "绩效 × 潜力矩阵", "风险关注与决策建议", "晋升智能助手与文档"]),
    ("王", "王浩阳", "员工档案与技能体系", "8081", "档案 · 技能",
     ["档案 CRUD / 批量与 Excel", "绩效、潜力、人才结构", "9 类 172 项技能树", "公共模块与网关治理"]),
    ("李", "李秉谦", "数据分析与报告", "8085", "看板 · 模型",
     ["梯队健康度与九宫格", "人才流动趋势与预测", "模型训练、日志与在线推理", "自定义报表与版本溯源"]),
    ("朱", "朱坤奇", "梯队规划与预测", "8082", "岗位 · 继任",
     ["72 个岗位画像", "继任候选与准备度", "人才池容量测算", "流失与关键岗位风险"]),
    ("班", "班火明", "智能培训推荐", "8083", "课程 · 路径",
     ["58 门课程库", "岗位必修差额推荐", "学习路径与培训记录", "前端交互与智能助手"]),
]
for i, (ini, nm, mod, port, kw, duties) in enumerate(MEMBERS):
    x = 0.78 + i * 2.38
    card = rect(s, x, 1.86, 2.24, 3.66, WHITE, LINE, 0.9, radius=0.07, shadow=True)
    rect(s, x, 1.86, 2.24, 0.055, PURPLE if i % 2 == 0 else INDIGO, None, radius=0.02)
    av = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x + 0.17), Inches(2.06), Inches(0.5), Inches(0.5))
    av.fill.solid(); av.fill.fore_color.rgb = PURPLE_PALE; av.line.fill.background(); av.shadow.inherit = False
    tf = av.text_frame; tf.vertical_anchor = MSO_ANCHOR.MIDDLE
    for attr in ("margin_left", "margin_right", "margin_top", "margin_bottom"):
        setattr(tf, attr, 0)
    p = tf.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
    r = p.add_run(); r.text = ini; _set_font(r, CN, 15, True, PURPLE)
    text(s, x + 0.76, 2.08, 1.35, 0.3, nm, 14, True, INK)
    text(s, x + 0.76, 2.4, 1.35, 0.22, "模块负责人", 7, False, FAINT)
    text(s, x + 0.17, 2.72, 1.95, 0.5, mod, 10, True, INK, line=1.2)
    chip = rect(s, x + 0.17, 3.24, 0.52, 0.21, LAV, None, radius=0.35)
    text(s, x + 0.17, 3.265, 0.52, 0.16, port, 7, True, PURPLE, align=PP_ALIGN.CENTER, font=NUM)
    text(s, x + 0.74, 3.27, 1.3, 0.18, kw, 7, False, MUTED)
    line(s, x + 0.17, 3.57, x + 2.07, 3.57, LINE, 0.8)
    ty = 3.68
    for d in duties:
        dot = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x + 0.19), Inches(ty + 0.085), Inches(0.045), Inches(0.045))
        dot.fill.solid(); dot.fill.fore_color.rgb = PURPLE_LT; dot.line.fill.background(); dot.shadow.inherit = False
        text(s, x + 0.3, ty, 1.82, 0.42, d, 7.6, False, MUTED, line=1.2)
        ty += 0.44
# bottom strips
p1 = rect(s, 0.78, 5.7, 7.45, 0.98, BG, None, radius=0.06)
text(s, 0.95, 5.8, 2.2, 0.22, "公共基础设施", 8, True, PURPLE, spc=1)
text(s, 0.95, 6.04, 7.1, 0.5, "talent-gateway 9090 · Nacos 2.4 注册配置 · Sentinel 限流熔断 · talent-common 统一返回 / 异常 / 分页", 8, False, GRAY, line=1.35)
p2 = rect(s, 8.4, 5.7, 4.22, 0.98, BG, None, radius=0.06)
text(s, 8.57, 5.8, 2.0, 0.22, "协作机制", 8, True, PURPLE, spc=1)
text(s, 8.57, 6.04, 3.9, 0.5, "Git feature 分支 · 一人一模块\n接口契约先行 · 网关统一入口", 8, False, GRAY, line=1.4)
notes(s, "团队 5 人一人一个模块：牛亚晨晋升、王浩阳员工档案与技能、李秉谦分析、朱坤奇梯队、班火明培训。网关和公共模块由王浩阳统一维护，避免冲突。")

# =====================================================================
# 04 purpose
# =====================================================================
s = header("02", "O B J E C T I V E", "把人才管理从「表格 + 经验」变成「可查询、可预测、可解释」",
           "同一批数据，既要回答「现在怎么样」，也要回答「接下来怎么办」")
ROWS = [
    ("01", "数据分散、口径不一", "档案、绩效、培训、岗位散在多张表，统计靠人工汇总",
     "29 张表统一建模", "1000 名员工 + 72 个岗位同库同口径，一次查询到底"),
    ("02", "梯队看不清、盘点靠人工", "关键岗位有没有接班人、缺口多大，说不清楚",
     "继任 + 人才池 + 风险一屏盘清", "72 个岗位、142 名核心骨干、335 名高潜实时可见"),
    ("03", "培养凭经验、课程乱推", "不知道谁缺什么、该学什么，培训与实际脱节",
     "岗位必修 − 已完成 = 待补", "按技能缺口推荐课程，58 门课程与岗位要求对齐"),
    ("04", "风险发现晚、晋升凭印象", "等员工提出离职才反应，晋升缺少量化依据",
     "风险预警 + 加权评分", "6 名高风险提前识别，晋升综合分可解释、可复核"),
]
y = 1.84
for i, (n, pain, pdesc, goal, gdesc) in enumerate(ROWS):
    text(s, 0.72, y + 0.02, 0.4, 0.3, n, 10, True, PURPLE_LT, font=NUM)
    text(s, 1.1, y, 2.55, 0.28, pain, 10.5, True, INK)
    text(s, 1.1, y + 0.3, 2.6, 0.5, pdesc, 7.8, False, MUTED, line=1.3)
    line(s, 3.72, y + 0.28, 3.98, y + 0.28, PURPLE_LT, 1.4, arrow=True)
    text(s, 4.12, y - 0.01, 3.25, 0.28, goal, 10.2, True, PURPLE)
    text(s, 4.12, y + 0.3, 3.45, 0.5, gdesc, 7.8, False, MUTED, line=1.3)
    if i < 3:
        line(s, 0.72, y + 0.9, 7.55, y + 0.9, LINE, 0.8)
    y += 0.96

# right meaning cards
text(s, 8.2, 1.82, 2.2, 0.26, "三 重 意 义", 7.5, True, PURPLE, spc=2.4)
meanings = [
    ("管理提效", "盘点从「两周 Excel」到「一屏看板」，1000 人实时聚合", "2 周 → 1 屏", GREEN, GREEN_PALE),
    ("风险前置", "把高风险、中风险名单前置到可干预的时间窗口", "6 + 104 人", AMBER, AMBER_PALE),
    ("决策公平", "规则、模型、数据版本三重留痕，晋升不再只看印象", "全程可追溯", PURPLE, PURPLE_PALE),
]
my = 2.14
for (t, d, stat, col, pale) in meanings:
    rect(s, 8.2, my, 4.42, 1.12, WHITE, LINE, 0.9, radius=0.06, shadow=True)
    rect(s, 8.2, my, 0.06, 1.12, col, None, radius=0.02)
    text(s, 8.42, my + 0.13, 2.4, 0.26, t, 10.5, True, INK)
    text(s, 8.42, my + 0.45, 2.55, 0.5, d, 7.6, False, MUTED, line=1.35)
    text(s, 11.1, my + 0.3, 1.35, 0.3, stat, 9.5, True, col, align=PP_ALIGN.RIGHT, font=NUM)
    my += 1.22

# value band
band = rect(s, 0.72, 5.86, 11.9, 0.92, INK, None, radius=0.05)
text(s, 0.98, 6.06, 0.55, 0.3, "价值", 8.5, True, PURPLE_LT, spc=1)
text(s, 1.57, 6.02, 10.8, 0.42, "让「谁该培养、谁有风险、谁可晋升」都能被数据回答，而不是被印象决定。", 13, True, WHITE)
notes(s, "目的与意义：四个痛点对应四个做法，右边三重意义是管理提效、风险前置、决策公平。一句话总结：让该培养、该预警、该晋升的人都能被数据回答。")

# =====================================================================
# 05 function tree
# =====================================================================
s = header("03", "F U N C T I O N   M A P", "平台拆为五大模块，每个模块都是「数据 + 分析 + AI 助手」的闭环",
           "功能树按业务域组织；模块之间通过网关与 Feign 共享数据，不跨库直连")
# root
root = rect(s, 4.33, 1.8, 4.68, 0.66, INK, None, radius=0.06)
text(s, 4.5, 1.9, 4.3, 0.3, "智能企业人才梯队建设平台", 13.5, True, WHITE, align=PP_ALIGN.CENTER)
text(s, 4.5, 2.18, 4.3, 0.2, "5 大业务模块 · 1 个统一入口 9090", 6.8, False, RGBColor(0x9C, 0xAE, 0xC6), align=PP_ALIGN.CENTER, font=NUM)
line(s, 6.66, 2.46, 6.66, 2.66, RGBColor(0xC4, 0xB5, 0xFD), 1.2)
centers = [1.90, 4.28, 6.66, 9.04, 11.42]
line(s, centers[0], 2.66, centers[-1], 2.66, RGBColor(0xC4, 0xB5, 0xFD), 1.2)
for cx in centers:
    line(s, cx, 2.66, cx, 2.86, RGBColor(0xC4, 0xB5, 0xFD), 1.2)

MODS = [
    ("员工档案智能管理", "8081 · 人事主数据", "档案 · 属性",
     ["员工名册：查询 / 新增 / 编辑", "绩效、潜力、人才结构", "9 类 172 项技能树", "Excel 导入导出 / 批量改删"]),
    ("梯队规划与预测", "8082 · 岗位与继任", "岗位 · 继任",
     ["72 个岗位画像与要求", "继任候选、匹配度与准备度", "人才池容量与缺口测算", "流失预警、关键岗位风险"]),
    ("智能培训推荐", "8083 · 课程与路径", "课程 · 路径",
     ["58 门课程库与分类筛选", "岗位必修 − 已完成 = 待补", "学习路径与阶段任务", "培训记录与效果分析"]),
    ("晋升决策支持", "8084 · 评分与建议", "评分 · 建议",
     ["跨层级候选人筛选", "综合评分与绩效潜力矩阵", "风险关注与决策建议", "晋升 AI 助手与理由生成"]),
    ("数据分析与报告", "8085 · 看板与模型", "看板 · 模型",
     ["梯队健康度与九宫格", "流动趋势与未来 6 个月预测", "模型训练、日志与在线推理", "自定义报表与版本溯源"]),
]
for i, (name, meta, tag, funcs) in enumerate(MODS):
    x = 0.78 + i * 2.38
    card = rect(s, x, 2.86, 2.24, 0.92, PURPLE_PALE, RGBColor(0xE2, 0xD5, 0xF7), 0.8, radius=0.06)
    text(s, x + 0.15, 2.95, 1.98, 0.4, name, 9.8, True, INK, line=1.15)
    text(s, x + 0.15, 3.4, 1.98, 0.2, meta, 6.8, False, PURPLE, font=NUM)
    yy = 3.98
    for f in funcs:
        dot = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x + 0.04), Inches(yy + 0.07), Inches(0.05), Inches(0.05))
        dot.fill.solid(); dot.fill.fore_color.rgb = PURPLE_MID; dot.line.fill.background(); dot.shadow.inherit = False
        text(s, x + 0.16, yy, 2.0, 0.46, f, 8.3, False, GRAY, line=1.25)
        yy += 0.52
rect(s, 0.78, 6.46, 11.76, 0.38, LAV, None, radius=0.3)
text(s, 0.95, 6.54, 11.4, 0.24, "横向能力：每个模块都配一个 Dify 智能助手（员工 / 规划 / 培训 / 晋升 / 分析），用自然语言完成查询、分析与解释。", 8, False, PURPLE)
notes(s, "功能树：平台分五大模块，每个模块都是数据 + 分析 + AI 助手的闭环。横向还有统一的智能助手能力。")

# =====================================================================
# 06 workflow
# =====================================================================
s = header("04", "C O R E   W O R K F L O W", "从一条员工数据，到一次可解释的人才决策",
           "六步闭环：建档 → 画像 → 盘点 → 培养 → 晋升 → 分析，结果再回流校准数据与模型")
STEPS = [
    ("01", "数据接入", "1000 员工 / 72 岗位\n29 张表统一入库"),
    ("02", "建档画像", "绩效·潜力·技能\n岗位要求标准化"),
    ("03", "梯队盘点", "继任候选 / 人才池\n健康度 88.6"),
    ("04", "培养补缺", "技能缺口 → 课程\n学习路径与记录"),
    ("05", "晋升决策", "规则评分 + 模型\n矩阵与决策建议"),
    ("06", "分析预测", "看板 / 流动预测\n报表与模型日志"),
]
bw, gap = 1.72, 0.3
x = 0.84
for i, (n, t, d) in enumerate(STEPS):
    rect(s, x, 2.2, bw, 1.5, WHITE, LINE, 0.9, radius=0.06, shadow=True)
    rect(s, x, 2.2, bw, 0.05, PURPLE if i % 2 == 0 else INDIGO, None, radius=0.02)
    text(s, x + 0.13, 2.31, 0.5, 0.24, n, 7.5, True, PURPLE_LT, font=NUM, spc=1)
    text(s, x + 0.13, 2.56, bw - 0.26, 0.28, t, 11, True, INK)
    text(s, x + 0.13, 2.92, bw - 0.24, 0.66, d, 7.4, False, MUTED, line=1.4)
    if i < 5:
        line(s, x + bw + 0.04, 2.95, x + bw + gap - 0.04, 2.95, RGBColor(0xB9, 0xA8, 0xE8), 1.6, arrow=True)
    x += bw + gap

# panels
rect(s, 0.72, 4.02, 5.78, 1.83, BG, None, radius=0.05)
text(s, 0.92, 4.15, 3.2, 0.26, "闭环：数据越用越准", 10, True, INK)
bul = ["入职 / 晋升 / 调岗 / 离职写入 emp_movement，流动趋势自动更新",
       "每份报表记录数据版本与模型版本，可回溯「数字怎么来的」",
       "训练与失败都写 model_log，线上只读最新成功版本"]
yy = 4.5
for b in bul:
    dot = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(0.94), Inches(yy + 0.09), Inches(0.055), Inches(0.055))
    dot.fill.solid(); dot.fill.fore_color.rgb = PURPLE_MID; dot.line.fill.background(); dot.shadow.inherit = False
    text(s, 1.08, yy, 5.24, 0.42, b, 8.2, False, GRAY, line=1.3)
    yy += 0.42

rect(s, 6.7, 4.02, 5.93, 1.83, BG, None, radius=0.05)
text(s, 6.9, 4.15, 4.4, 0.26, "贯穿：每个模块都有一个 AI 助手", 10, True, INK)
agents = ["证据汇总", "脱敏裁剪", "上下文注入", "对话解释"]
ax = 6.92
for i, a in enumerate(agents):
    rect(s, ax, 4.6, 1.24, 0.4, WHITE, RGBColor(0xDD, 0xD6, 0xFE), 0.8, radius=0.3)
    text(s, ax, 4.68, 1.24, 0.24, a, 7.6, True, PURPLE, align=PP_ALIGN.CENTER)
    if i < 3:
        line(s, ax + 1.26, 4.8, ax + 1.44, 4.8, RGBColor(0xB9, 0xA8, 0xE8), 1.2, arrow=True)
    ax += 1.47
text(s, 6.92, 5.2, 5.5, 0.5, "后端先算好事实，再把裁剪后的只读快照交给 Dify；模型只负责解释，不直接改数据。", 7.8, False, MUTED, line=1.4)
text(s, 0.72, 6.12, 11.9, 0.34, "业务规则在 Service 层、模型参数在 model_artifact、SQL 只负责取数——三层职责分开，流程才可维护。", 9, False, FAINT, italic=True)
notes(s, "核心流程六步：数据接入、建档画像、梯队盘点、培养补缺、晋升决策、分析预测。下面两个闭环：数据回流让趋势越来越准，AI 助手贯穿每一步。")

# =====================================================================
# 07 architecture
# =====================================================================
s = header("05", "A R C H I T E C T U R E", "四层架构：前端 → 网关 → 五个微服务 → 数据与能力",
           "服务独立部署、统一入口；跨模块数据通过网关聚合与 OpenFeign 流转")
# layer labels
for (lab, yy, hh) in [("展示层", 1.82, 0.86), ("接入层", 2.88, 0.76), ("服务层", 3.86, 1.36), ("数据与\n智能层", 5.42, 1.0)]:
    text(s, 0.7, yy + 0.06, 0.9, hh, lab, 8.5, True, MUTED, align=PP_ALIGN.LEFT, line=1.15)
    line(s, 0.7, yy + hh + 0.02, 0.7, yy + hh + 0.02, LINE, 0.8)

# 展示层
rect(s, 1.72, 1.82, 10.9, 0.86, RGBColor(0xF5, 0xF3, 0xFF), RGBColor(0xDD, 0xD6, 0xFE), 1, radius=0.05)
text(s, 1.95, 1.95, 10.4, 0.28, "Vue 3 + TypeScript + Element Plus｜六个功能页 + 五个智能助手", 11, True, INK)
text(s, 1.95, 2.26, 10.4, 0.28, "员工档案 · 技能体系 · 梯队规划 · 智能培训 · 晋升决策 · 数据分析（按 Tab 懒加载）", 7.8, False, MUTED)
# 接入层
rect(s, 1.72, 2.88, 10.9, 0.76, RGBColor(0xEE, 0xF2, 0xFF), RGBColor(0xC7, 0xD2, 0xFE), 1, radius=0.05)
text(s, 1.95, 2.99, 10.4, 0.28, "API 网关 talent-gateway : 9090｜/api/** 统一路由 · Sentinel 限流熔断 · 统一 Result 与异常", 10.5, True, INDIGO)
text(s, 1.95, 3.3, 10.4, 0.24, "前端只连一个入口；限流返回 429、降级返回 503，页面有明确提示", 7.6, False, MUTED)
# 服务层 boxes
svc = [("员工档案", "8081", "档案 / 绩效 / 技能"), ("梯队规划", "8082", "岗位 / 继任 / 风险"),
       ("智能培训", "8083", "课程 / 路径 / 记录"), ("晋升决策", "8084", "候选 / 评分 / 建议"),
       ("数据分析", "8085", "看板 / 模型 / 报表")]
sx = 1.78
sw = 1.98
for i, (nm, port, d) in enumerate(svc):
    rect(s, sx, 3.86, sw, 1.36, WHITE, LINE, 0.9, radius=0.05, shadow=True)
    rect(s, sx, 3.86, sw, 0.05, PURPLE if i != 4 else INDIGO, None, radius=0.02)
    text(s, sx + 0.12, 3.99, sw - 0.2, 0.28, nm, 10, True, INK)
    text(s, sx + 0.12, 4.28, 0.8, 0.22, port, 8, True, PURPLE, font=NUM)
    text(s, sx + 0.12, 4.62, sw - 0.22, 0.4, d, 7, False, MUTED, line=1.3)
    sx += sw + 0.2
# arrows
for yy1, yy2 in [(2.68, 2.86), (3.64, 3.84)]:
    line(s, 7.2, yy1, 7.2, yy2, RGBColor(0xB9, 0xA8, 0xE8), 1.4, arrow=True)
line(s, 7.2, 5.22, 7.2, 5.4, RGBColor(0xB9, 0xA8, 0xE8), 1.4, arrow=True)
# 数据层
data = [("MySQL 8 · talent_db", "29 张表 / 1000 员工", RGBColor(0xF8, 0xFA, 0xFC), LINE),
        ("Nacos 2.4 · 8848", "注册中心 + 配置中心", RGBColor(0xF8, 0xFA, 0xFC), LINE),
        ("Dify Cloud", "5 个工作流智能体", CYAN_PALE, RGBColor(0xA5, 0xF3, 0xFC)),
        ("model_artifact", "模型参数 / 版本 / 指标", RGBColor(0xF8, 0xFA, 0xFC), LINE)]
dx = 1.72
dw = 2.62
for i, (nm, d, fill, ln) in enumerate(data):
    rect(s, dx, 5.42, dw, 0.96, fill, ln, 0.9, radius=0.05)
    text(s, dx + 0.14, 5.54, dw - 0.25, 0.26, nm, 9.3, True, INK)
    text(s, dx + 0.14, 5.85, dw - 0.25, 0.4, d, 7.2, False, MUTED, line=1.3)
    dx += dw + 0.17
text(s, 0.72, 6.6, 11.9, 0.3, "跨模块：梯队 → 员工（Feign 取档案）/ 晋升 → 梯队 + 技能 / 培训 → 岗位必修；Dify 仅出站 HTTPS，本地接口不回传公网。", 7.8, False, FAINT)
notes(s, "架构四层：展示层、网关、五个服务、数据与智能层。强调两个点：前端只连 9090；Dify 只出站 HTTPS，不回调内网，所以不需要公网隧道。")

# =====================================================================
# 08 core tech
# =====================================================================
s = header("05", "T E C H N O L O G Y", "微服务底座 + 数据与规则 + 机器学习 + 大模型智能体",
           "没有为了技术而技术：每一层都对应一个真实问题")
chips = ["JDK 17", "Spring Boot 3.2", "Spring Cloud 2023", "Nacos 2.4", "MySQL 8",
         "MyBatis-Plus", "Vue 3 + TS", "Dify 工作流"]
cx = 0.7
for c in chips:
    wch = 0.22 + len(c) * 0.085
    rect(s, cx, 1.78, wch, 0.3, BG, LINE, 0.7, radius=0.3)
    text(s, cx, 1.84, wch, 0.2, c, 7.2, True, GRAY, align=PP_ALIGN.CENTER, font=NUM)
    cx += wch + 0.09

CARDS = [
    ("服", "微服务与治理", "6 个服务独立部署；Nacos 负责注册与配置，Gateway 按模块路由；Sentinel 限流、Feign 降级兜底。改配置不用改代码。", "talent-gateway · Sentinel · OpenFeign"),
    ("数", "数据建模与查询", "29 张表按员工、岗位、技能、培训四个域拆分；分页插件避免全表扫描，复杂指标用注解 SQL 聚合。员工与岗位共用技能、课程字典。", "emp_* / pos_* / promo_* / report_*"),
    ("模", "机器学习工程", "标准化逻辑回归，训练 / 测试 / 版本 / 部署全链路；AUC、Precision、Recall、F1 随版本留存。训练失败也写日志，线上不会读到半成品。", "attrition · potential · position-fit"),
    ("智", "大模型智能体", "5 个 Dify 工作流；后端注入只读上下文，字段脱敏；未配置时明确降级。对话只读，不触发新增、修改、删除。", "5 个 AssistantServiceImpl"),
    ("表", "Excel 与批处理", "EasyExcel 模板下载、批量导入、流式导出；导出筛选后的全量数据，失败行按行号回执。导入模板带示例与校验。", "commons-io 2.16.1 · 流式写出"),
    ("视", "前端可视化", "Vue 3 组合式 API + Element Plus；九宫格、矩阵、进度条、趋势图多形态，按 Tab 懒加载。页面与接口一模块一文件，便于分工。", "6 个页面 · 36 个 Tab"),
]
for i, (ic, t, d, ev) in enumerate(CARDS):
    col, row = i % 3, i // 3
    x = 0.68 + col * 4.05
    y = 2.32 + row * 2.06
    rect(s, x, y, 3.86, 1.92, WHITE, LINE, 0.9, radius=0.06, shadow=True)
    sq = rect(s, x + 0.17, y + 0.18, 0.4, 0.4, PURPLE_PALE if i % 2 == 0 else LAV, None, radius=0.22)
    text(s, x + 0.17, y + 0.235, 0.4, 0.28, ic, 11, True, PURPLE, align=PP_ALIGN.CENTER)
    text(s, x + 0.68, y + 0.2, 3.0, 0.3, t, 11.2, True, INK)
    text(s, x + 0.17, y + 0.67, 3.52, 0.88, d, 7.8, False, GRAY, line=1.4)
    line(s, x + 0.17, y + 1.48, x + 3.69, y + 1.48, LINE, 0.7)
    text(s, x + 0.17, y + 1.55, 3.52, 0.24, ev, 6.6, False, PURPLE, font=NUM)
text(s, 0.7, 6.6, 11.9, 0.28, "技术选型原则：业务规则放 Service、模型参数进数据库、SQL 只负责取数。", 8.5, False, FAINT, italic=True)
notes(s, "核心技术六块：微服务治理、数据建模、机器学习、大模型智能体、Excel 批处理、前端可视化。不是堆技术，每一块都对应具体问题。")

# =====================================================================
# 09 models
# =====================================================================
s = header("05", "M A C H I N E   L E A R N I N G", "三个可训练、可部署、可解释的人才模型",
           "标准化逻辑回归 + 统一特征工程；模型参数落库，线上只读最新成功版本")
MODELS = [
    ("人才流失风险预测", "v1.6", "0.8172", "分类模型 · 1000 样本 / 110 正样本 · 7 个特征", 0.817),
    ("高潜人才识别", "v1.3", "0.9967", "分类模型 · 1000 样本 / 335 正样本 · 7 个特征", 0.997),
    ("岗位匹配度推荐", "v1.3", "0.9816", "排序模型 · 4000 样本 / 1000 正样本 · 8 个特征", 0.982),
]
y = 1.84
for (nm, ver, auc, meta, ratio) in MODELS:
    rect(s, 0.7, y, 5.55, 1.24, WHITE, LINE, 0.9, radius=0.06, shadow=True)
    text(s, 0.9, y + 0.15, 2.65, 0.28, nm, 10, True, INK)
    chip = rect(s, 3.42, y + 0.16, 0.52, 0.22, LAV, None, radius=0.3)
    text(s, 3.42, y + 0.195, 0.52, 0.16, ver, 6.8, True, PURPLE, align=PP_ALIGN.CENTER, font=NUM)
    text(s, 4.7, y + 0.1, 1.35, 0.36, auc, 16, True, PURPLE, align=PP_ALIGN.RIGHT, font=NUM)
    text(s, 4.55, y + 0.44, 1.5, 0.2, "AUC", 6.5, False, FAINT, align=PP_ALIGN.RIGHT, font=NUM, spc=1)
    # bar
    rect(s, 0.9, y + 0.72, 3.6, 0.07, RGBColor(0xEE, 0xEB, 0xF7), None, radius=0.35)
    rect(s, 0.9, y + 0.72, 3.6 * ratio, 0.07, PURPLE_MID, None, radius=0.35)
    text(s, 0.9, y + 0.9, 4.9, 0.24, meta, 6.8, False, MUTED)
    y += 1.34

# pipeline
text(s, 6.62, 1.82, 2.4, 0.24, "从训练到推理", 8, True, PURPLE, spc=1.5)
PIPE = [
    ("特征构建", "司龄 / 绩效 / 薪酬系数 / 潜力 / 技能数 / 培训数 / 年龄；岗位匹配另加覆盖率与层级匹配"),
    ("标准化", "均值与标准差随模型一起落库，线上用同一套参数做 z-score，避免训练/推理不一致"),
    ("训练与评估", "按时间切分训练 / 测试集，输出 AUC、Precision、Recall、F1 与样本量"),
    ("阈值与解释", "按业务阈值判定风险等级，计算各特征贡献，输出 Top 影响因素"),
    ("在线推理", "读取最新成功版本并缓存 artifact；模型或数据版本随预测结果一起返回"),
]
yy = 2.12
for i, (t, d) in enumerate(PIPE):
    circle_num(s, 6.64, yy + 0.02, 0.28, i + 1, PURPLE if i < 4 else INDIGO, WHITE, 8)
    text(s, 7.04, yy, 5.4, 0.24, t, 9.3, True, INK)
    text(s, 7.04, yy + 0.27, 5.45, 0.38, d, 7.4, False, MUTED, line=1.35)
    if i < 4:
        line(s, 6.78, yy + 0.32, 6.78, yy + 0.64, RGBColor(0xDD, 0xD6, 0xFE), 1)
    yy += 0.68

# stats band
rect(s, 0.7, 5.84, 11.93, 0.5, BG, None, radius=0.3)
stats = [("20", "训练次数"), ("19", "成功次数"), ("95%", "成功率"), ("0.9013", "平均 AUC"), ("2026-11-03", "最近训练")]
for i, (v, k) in enumerate(stats):
    xx = 0.95 + i * 2.36
    text(s, xx, 5.88, 2.15, 0.3, v, 12.5, True, PURPLE, font=NUM)
    text(s, xx, 6.16, 2.1, 0.2, k, 7, False, MUTED)
text(s, 0.7, 6.55, 11.93, 0.42, "时间序列预测采用近 24 个月季节调整线性趋势；离职 / 晋升历史事件不足时改用风险池、高潜池业务基线，并明确标注低置信度。", 7.6, False, FAINT, line=1.3)
notes(s, "模型是加分项：三个模型都能训练、能部署、能在页面推理。流失 0.82、高潜 0.997、岗位匹配 0.98。注意高潜 AUC 高是因为标签来自潜力评级，特征和标签有相关性，汇报时可以说清楚。")

# =====================================================================
# 10 highlights
# =====================================================================
s = header("06", "H I G H L I G H T S", "五个特色：把「能看、能算、能问」做成可复用的工程能力",
           "技术特色与业务特色在同一条链路上闭环")
# left big card
rect(s, 0.7, 1.82, 5.2, 4.9, INK, None, radius=0.05)
text(s, 0.94, 2.0, 1.2, 0.24, "01", 10, True, PURPLE_LT, font=NUM, spc=1)
text(s, 1.34, 1.98, 4.2, 0.34, "全模块统一的 AI 助手", 14.5, True, WHITE)
text(s, 0.94, 2.42, 4.7, 0.6, "五个模块各配一个 Dify 智能体，共用同一套「证据 → 脱敏 → 注入 → 解释」范式，交互一致、密钥只在服务端。", 8, False, RGBColor(0xB4, 0xC1, 0xD4), line=1.5)
steps = [("业务证据汇总", "SQL / 规则 / 模型结果先算好"),
         ("字段脱敏裁剪", "手机号、薪酬、年龄不出库"),
         ("注入只读上下文", "拼成结构化 evidence 快照"),
         ("Dify 生成解释", "引用事实回答，不直接改数据")]
yy = 3.2
for i, (t, d) in enumerate(steps):
    rect(s, 0.94, yy, 4.72, 0.62, RGBColor(0x16, 0x21, 0x38), RGBColor(0x2B, 0x36, 0x50), 0.8, radius=0.06)
    circle_num(s, 1.06, yy + 0.17, 0.28, i + 1, PURPLE_MID, WHITE, 7.5)
    text(s, 1.45, yy + 0.09, 2.1, 0.24, t, 8.8, True, WHITE)
    text(s, 1.45, yy + 0.34, 3.0, 0.22, d, 6.8, False, RGBColor(0x8F, 0xA0, 0xB8))
    yy += 0.72
text(s, 0.94, 6.12, 4.7, 0.42, "5 个智能体　·　未配置即降级　·　本地接口不上公网", 7.2, False, PURPLE_LT, font=NUM)
# right 2x2
FEATS = [
    ("02", "规则 + 模型双轨", "晋升用加权规则给分（绩效 / 潜力 / 司龄），流失用模型给概率；结论能落到具体特征，不黑箱。", "PromotionMapper · TalentModelServiceImpl"),
    ("03", "跨模块数据打通", "晋升读岗位与继任，培训读技能缺口，分析聚合五模块；只走网关与 Feign，不跨库直连。", "EmployeeClient(Feign) · 网关聚合"),
    ("04", "隐私与降级", "只传决策必需字段；Dify 不可用时给出明确提示，业务页面照常可用，不把鸡蛋放一个篮子。", "sanitize* 脱敏 · 环境变量开关"),
    ("05", "数据可溯源", "报表带数据版本 + 模型版本，训练日志记录参数与指标，能回答「这个数字是哪份数据、哪个模型算的」。", "report.dataset_version / model_version"),
]
for i, (n, t, d, ev) in enumerate(FEATS):
    col, row = i % 2, i // 2
    x = 6.12 + col * 3.26
    y = 1.82 + row * 1.72
    rect(s, x, y, 3.12, 1.56, WHITE, LINE, 0.9, radius=0.06, shadow=True)
    text(s, x + 0.16, y + 0.13, 0.4, 0.2, n, 7.5, True, PURPLE_LT, font=NUM)
    text(s, x + 0.58, y + 0.11, 2.4, 0.26, t, 9.8, True, INK)
    text(s, x + 0.16, y + 0.46, 2.82, 0.82, d, 7.2, False, MUTED, line=1.36)
    line(s, x + 0.16, y + 1.2, x + 2.96, y + 1.2, LINE, 0.7)
    text(s, x + 0.16, y + 1.27, 2.82, 0.18, ev, 5.9, False, PURPLE, font=NUM)
# quote
rect(s, 6.12, 5.3, 6.5, 1.34, BG, None, radius=0.05)
text(s, 6.32, 5.46, 6.1, 0.3, "特色不是功能堆叠。", 10, True, PURPLE)
text(s, 6.32, 5.76, 6.05, 0.72, "同一套范式复用到 5 个模块，再增加一个模块时，只需要补业务证据和页面，不用重写智能体链路。", 7.8, False, GRAY, line=1.45)
notes(s, "特色五个：统一 AI 助手、规则+模型双轨、跨模块打通、隐私与降级、数据可溯源。重点讲 AI 助手的四步范式，以及它为什么可复用。")

# =====================================================================
# 11-13 screenshots
# =====================================================================
s = header("07", "S C R E E N S H O T S", "核心功能截图（一）· 人才盘点与模型效果",
           "真实运行环境截取，仅作功能展示，讲解时略过")
pic_card(s, "dashboard_pipeline.png", 0.55, 1.94, 6.0, 3.75, "图 11-1　梯队整体状态：健康度 88.6、绩效 × 潜力九宫格与结构分布", "看板")
pic_card(s, "dashboard_model.png", 6.85, 1.94, 6.0, 3.75, "图 11-2　模型效果与日志：训练、评估、版本与在线推理入口", "模型")
text(s, 0.7, 6.18, 11.9, 0.45, "1000 名员工、72 个岗位全部来自 MySQL；页面数字由分析服务实时聚合，不是静态示例数据。", 8.5, False, FAINT, align=PP_ALIGN.CENTER)
notes(s, "这页只展示，不展开讲。左边是人才盘点看板，右边是模型效果与日志。")

s = header("07", "S C R E E N S H O T S", "核心功能截图（二）· 档案与技能",
           "真实运行环境截取，仅作功能展示，讲解时略过")
pic_card(s, "employee_roster.png", 0.55, 1.94, 6.0, 3.75, "图 12-1　员工名册：组合筛选、批量改删、Excel 导入导出", "档案")
pic_card(s, "skill_overview.png", 6.85, 1.94, 6.0, 3.75, "图 12-2　技能体系：9 类 172 项，谁在会、多少岗位要求", "技能")
text(s, 0.7, 6.18, 11.9, 0.45, "员工与岗位共用一套技能、课程字典，匹配才有意义；删除被引用的技能会被拒绝并说明原因。", 8.5, False, FAINT, align=PP_ALIGN.CENTER)
notes(s, "这页也只展示。员工档案支持批量操作和 Excel；技能树 9 类 172 项，是后续匹配的地基。")

s = header("07", "S C R E E N S H O T S", "核心功能截图（三）· 梯队、培训与晋升",
           "真实运行环境截取，仅作功能展示，讲解时略过")
pic_card(s, "planning_succession.png", 0.46, 1.98, 3.95, 2.47, "图 13-1　继任候选：匹配度、准备度与缺失技能", "梯队")
pic_card(s, "training_courses.png", 4.63, 1.98, 3.95, 2.47, "图 13-2　课程库：58 门课程，按类型与难度管理", "培训")
pic_card(s, "promotion_candidates.png", 8.8, 1.98, 3.95, 2.47, "图 13-3　晋升候选：综合分排序与风险提示", "晋升")
# bottom insights
ins = [("梯队", "岗位有画像、候选有准备度", "继任 / 人才池 / 风险三条线汇到一张看板"),
       ("培训", "推荐逻辑只有一条", "岗位必修 − 已完成 = 待补，课程与岗位对齐"),
       ("晋升", "综合分 = 绩效 × 15 + 潜力 × 0.5 + 司龄 × 2", "规则公开，面试之前先有量化依据")]
ix = 0.7
for i, (t, d1, d2) in enumerate(ins):
    rect(s, ix, 4.92, 3.82, 1.06, BG, None, radius=0.05)
    text(s, ix + 0.16, 5.02, 1.2, 0.24, t, 8.5, True, PURPLE, spc=1)
    text(s, ix + 0.16, 5.28, 3.5, 0.28, d1, 7.6, True, GRAY)
    text(s, ix + 0.16, 5.58, 3.5, 0.3, d2, 7.1, False, MUTED)
    ix += 3.95
text(s, 0.7, 6.4, 11.9, 0.32, "三个模块共用同一套岗位、技能、员工数据：梯队看候选，培训补缺口，晋升做决策。", 8.5, False, FAINT, align=PP_ALIGN.CENTER)
notes(s, "最后一页截图：梯队继任、培训课程库、晋升候选。三个模块共用同一套岗位和技能数据。")

# =====================================================================
# 14 summary
# =====================================================================
s = header("08", "S U M M A R Y", "从能跑到好用：一次完整的微服务 + 大模型工程实践",
           "五大模块上线、数据贯通、模型可训、助手可用——也留下了下一步")
stats = [("5", "业务模块"), ("6", "独立服务"), ("29", "张表"), ("1000", "名员工"), ("1.86", "万行代码")]
x = 0.7
for i, (v, k) in enumerate(stats):
    rect(s, x, 1.8, 2.26, 0.86, BG, None, radius=0.05)
    text(s, x + 0.16, 1.88, 1.0, 0.36, v, 17, True, PURPLE, font=NUM)
    text(s, x + 0.16, 2.31, 1.9, 0.2, k, 7.2, False, MUTED)
    x += 2.38

COLS = [
    ("完成情况", "DELIVERED", [
        "5 个业务服务 + 网关全部跑通，统一 9090 入口",
        "1000 名员工 / 72 个岗位 / 29 张表数据贯通",
        "3 类模型可训练、可部署、可在线推理（平均 AUC 0.90）",
        "5 个 Dify 智能体覆盖档案、规划、培训、晋升、分析",
        "报表与模型日志支持数据版本 / 模型版本回溯",
    ], GREEN),
    ("能力收获", "LEARNED", [
        "微服务拆分、注册配置、网关治理与限流降级",
        "把业务指标翻译成 SQL、规则与模型特征",
        "大模型应用工程化：注入、脱敏、降级、可观测",
        "用图表、流程图和统一叙事讲清复杂系统",
        "在 5 人协作里坚持接口契约和分支隔离",
    ], PURPLE),
    ("不足与展望", "NEXT", [
        "模型特征仍偏静态，行为数据接入不足",
        "权限与字段级脱敏需按角色细化",
        "离职 / 晋升事件少，时间序列置信度偏低",
        "移动端与预警推送尚未覆盖",
        "报表模板的维度组合还可以更自由",
    ], AMBER),
]
for i, (t, e, items, col) in enumerate(COLS):
    x = 0.7 + i * 4.02
    text(s, x, 2.92, 2.3, 0.28, t, 11, True, INK)
    text(s, x + 1.0, 2.98, 1.5, 0.2, e, 6.5, False, FAINT, font=NUM, spc=1.4)
    line(s, x, 3.26, x + 3.75, 3.26, LINE, 1)
    yy = 3.4
    for it in items:
        dot = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x), Inches(yy + 0.085), Inches(0.05), Inches(0.05))
        dot.fill.solid(); dot.fill.fore_color.rgb = col; dot.line.fill.background(); dot.shadow.inherit = False
        text(s, x + 0.14, yy, 3.55, 0.44, it, 7.9, False, GRAY, line=1.35)
        yy += 0.47

band = rect(s, 0.7, 6.14, 11.93, 0.66, INK, None, radius=0.05)
text(s, 0.94, 6.28, 1.4, 0.24, "下一步", 8, True, PURPLE_LT, spc=1)
text(s, 1.75, 6.26, 10.6, 0.34, "补权限体系与数据治理，把「能看、能算、能问」继续做成「能预警、能闭环」。", 10.5, True, WHITE)
notes(s, "总结：五个模块上线、数据贯通、模型可训、助手可用。也诚实说不足：特征偏静态、权限没细化、事件数据少。下一步补权限和数据治理。")

# =====================================================================
# 15 closing
# =====================================================================
page = 15
s = prs.slides.add_slide(BLANK)
bg = s.background.fill; bg.solid(); bg.fore_color.rgb = DARK_BG
for (x, y, d, col) in [(-1.6, -2.2, 7.4, RGBColor(0x22, 0x1B, 0x3A)), (9.0, -2.8, 8.4, RGBColor(0x2A, 0x1F, 0x4A))]:
    r = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x), Inches(y), Inches(d), Inches(d))
    r.fill.background(); r.line.color.rgb = col; r.line.width = Pt(1); r.shadow.inherit = False
logo(s, 6.25, 1.28, 0.84)
text(s, 0.7, 2.52, 11.93, 0.8, "感 谢 聆 听", 40, True, WHITE, align=PP_ALIGN.CENTER)
text(s, 0.7, 3.42, 11.93, 0.34, "请各位老师批评指正", 13, False, RGBColor(0xA9, 0xB6, 0xC9), align=PP_ALIGN.CENTER)
line(s, 5.55, 4.02, 7.78, 4.02, RGBColor(0x4A, 0x39, 0x74), 1)
text(s, 0.7, 4.28, 11.93, 0.3, "牛亚晨　王浩阳　李秉谦　朱坤奇　班火明", 11, False, RGBColor(0xD5, 0xDD, 0xE8), align=PP_ALIGN.CENTER)
text(s, 0.7, 4.72, 11.93, 0.28, "天津工业大学软件学院 · 融创软通-大模型技术应用-23级工大班", 8.5, False, RGBColor(0x7C, 0x8D, 0xA6), align=PP_ALIGN.CENTER)
text(s, 0.7, 5.12, 11.93, 0.28, "2026 年 11 月", 9, False, RGBColor(0x6B, 0x7F, 0xA0), align=PP_ALIGN.CENTER, font=NUM, spc=1)
notes(s, "结束页，感谢老师。")

cp = prs.core_properties
cp.title = "智能企业人才梯队建设平台 · 项目实训最终报告"
cp.subject = "天津工业大学软件学院 · 项目实训总结汇报"
cp.author = "牛亚晨 王浩阳 李秉谦 朱坤奇 班火明"
cp.keywords = "微服务, Spring Cloud, 人才梯队, 机器学习, Dify, 大模型"
cp.comments = "16:9，15 页；按任务书大纲组织：团队、目的、功能树、流程、技术、特色、截图、总结。"

prs.save(OUT)
print("saved:", OUT, "slides:", len(prs.slides))
