# -*- coding: utf-8 -*-
"""生成「晋升决策模块结构图」JPG（Pillow 绘制，三列紧凑树状图，保证插入 A4 后可读）。"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = r"D:\202609shixun\shixun002\晋升决策模块结构图.jpg"
FONT_DIR = r"C:\Windows\Fonts"

def pick(cands):
    for c in cands:
        p = os.path.join(FONT_DIR, c)
        if os.path.exists(p):
            return p
    return None

REG = pick(["msyh.ttc", "simhei.ttf", "msyh.ttf", "simsun.ttc"])
BOLD = pick(["msyhbd.ttc", "msyh.ttc", "simhei.ttf"])
assert REG

W, H = 1090, 930
BG = (255, 255, 255)
LINE = (148, 163, 184)

ROOT_FILL, ROOT_TEXT = (29, 78, 216), (255, 255, 255)
CAT_FILL, CAT_BORDER, CAT_TEXT, CAT_DESC = (219, 234, 254), (59, 130, 246), (30, 58, 138), (37, 99, 235)
LEAF_FILL, LEAF_BORDER, LEAF_TEXT, LEAF_DESC = (255, 255, 255), (148, 163, 184), (30, 41, 59), (100, 116, 139)

img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

f_title = ImageFont.truetype(BOLD, 32)
f_sub = ImageFont.truetype(REG, 16)

X = {"root": 160, "pkg": 485, "cls": 845}
BW = {"root": 300, "pkg": 210, "cls": 380}
BH = {"root": 60, "pkg": 54, "cls": 56}

def N(col, y, kind, name, desc=""):
    return dict(x=X[col], y=y, w=BW[col], h=BH[kind], kind=kind, name=name, desc=desc)

# ---- 节点 ----
root = N("root", 528, "root", "PromotionServiceApplication", "启动类 · @EnableDiscoveryClient")

controller = N("pkg", 182, "pkg", "controller", "REST 接口层")
service    = N("pkg", 406, "pkg", "service", "业务逻辑层")
mapper     = N("pkg", 598, "pkg", "mapper", "数据访问层")
dto        = N("pkg", 662, "pkg", "dto", "入参对象")
vo         = N("pkg", 790, "pkg", "vo", "出参对象")

rows = [
    (controller, N("cls", 150, "cls", "PromotionController", "/api/promotion/candidates")),
    (controller, N("cls", 214, "cls", "PromotionAssistantController", "/api/promotion/ai/{status, chat}")),
    (service,    N("cls", 278, "cls", "PromotionService", "候选人查询接口")),
    (service,    N("cls", 342, "cls", "PromotionAssistantService", "智能体接口")),
    (service,    N("cls", 406, "cls", "PromotionServiceImpl", "impl · 候选人业务实现")),
    (service,    N("cls", 470, "cls", "PromotionAssistantServiceImpl", "impl · Dify 工作流代理")),
    (service,    N("cls", 534, "cls", "PromotionEvidenceService", "impl · 证据汇总与排序")),
    (mapper,     N("cls", 598, "cls", "PromotionMapper", "@Select 候选筛选 SQL")),
    (dto,        N("cls", 662, "cls", "AssistantChatDTO", "对话入参")),
    (vo,         N("cls", 726, "cls", "PromotionCandidateVO", "候选人出参")),
    (vo,         N("cls", 790, "cls", "AssistantReplyVO", "智能体回复")),
    (vo,         N("cls", 854, "cls", "AssistantStatusVO", "配置状态")),
]

groups = {}
for p, c in rows:
    groups.setdefault(id(p), (p, []))[1].append(c)

def connect(p, children):
    pr = p["x"] + p["w"] // 2
    trunk = min(c["x"] - c["w"] // 2 for c in children) - 28
    d.line([(pr, p["y"]), (trunk, p["y"])], fill=LINE, width=2, joint="curve")
    ys = [c["y"] for c in children]
    d.line([(trunk, min(ys)), (trunk, max(ys))], fill=LINE, width=2, joint="curve")
    for c in children:
        cl = c["x"] - c["w"] // 2
        d.line([(trunk, c["y"]), (cl, c["y"])], fill=LINE, width=2, joint="curve")
        d.ellipse([cl - 3, c["y"] - 3, cl + 3, c["y"] + 3], fill=LINE)

for p, children in groups.values():
    connect(p, children)

def fit(text, path, size, maxw, min_size=9):
    s = size
    while s > min_size:
        f = ImageFont.truetype(path, s)
        if d.textlength(text, font=f) <= maxw:
            return f
        s -= 1
    return ImageFont.truetype(path, min_size)

def draw(n):
    l, t, r, b = n["x"] - n["w"] // 2, n["y"] - n["h"] // 2, n["x"] + n["w"] // 2, n["y"] + n["h"] // 2
    if n["kind"] == "root":
        fill, border, tc, dc = ROOT_FILL, ROOT_FILL, ROOT_TEXT, (219, 234, 254)
    elif n["kind"] == "pkg":
        fill, border, tc, dc = CAT_FILL, CAT_BORDER, CAT_TEXT, CAT_DESC
    else:
        fill, border, tc, dc = LEAF_FILL, LEAF_BORDER, LEAF_TEXT, LEAF_DESC
    d.rounded_rectangle([l, t, r, b], radius=9, fill=fill, outline=border, width=2)

    maxw = n["w"] - 18
    if n["kind"] == "cls":
        nf = fit(n["name"], BOLD, 21, maxw)
        df = fit(n["desc"], REG, 14, maxw)
        d.text((n["x"], n["y"] - 9), n["name"], font=nf, fill=tc, anchor="mm")
        d.text((n["x"], n["y"] + 12), n["desc"], font=df, fill=dc, anchor="mm")
    elif n["kind"] == "pkg":
        nf = fit(n["name"], BOLD, 22, maxw)
        df = fit(n["desc"], REG, 14, maxw)
        d.text((n["x"], n["y"] - 8), n["name"], font=nf, fill=tc, anchor="mm")
        d.text((n["x"], n["y"] + 12), n["desc"], font=df, fill=dc, anchor="mm")
    else:
        nf = fit(n["name"], BOLD, 20, maxw)
        df = fit(n["desc"], REG, 13, maxw)
        d.text((n["x"], n["y"] - 8), n["name"], font=nf, fill=tc, anchor="mm")
        d.text((n["x"], n["y"] + 12), n["desc"], font=df, fill=dc, anchor="mm")

for n in [root, controller, service, mapper, dto, vo] + [c for _, c in rows]:
    draw(n)

d.text((W // 2, 40), "晋升决策模块结构图", font=f_title, fill=(15, 23, 42), anchor="mm")
d.text((W // 2, 74), "com.talent.promotion · 模块四 晋升决策支持（端口 8084）",
       font=f_sub, fill=(100, 116, 139), anchor="mm")

img.save(OUT, "JPEG", quality=95, subsampling=0)
print("saved:", OUT, img.size)
