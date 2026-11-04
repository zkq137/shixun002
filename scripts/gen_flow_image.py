# -*- coding: utf-8 -*-
"""生成「晋升决策模块程序流程图」JPG（两条主流程：候选人查询 / AI 助手对话）。"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = r"D:\202609shixun\shixun002\晋升决策模块程序流程图.jpg"
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

W, H = 1040, 900
BG = (255, 255, 255)

START_FILL, START_BORDER, START_TEXT = (220, 252, 231), (34, 197, 94), (22, 101, 52)
PROC_FILL, PROC_BORDER, PROC_TEXT, PROC_DESC = (255, 255, 255), (148, 163, 184), (30, 41, 59), (100, 116, 139)
EXT_FILL, EXT_BORDER, EXT_TEXT, EXT_DESC = (254, 243, 199), (245, 158, 11), (120, 53, 15), (146, 64, 14)
ARROW = (100, 116, 139)

img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

f_title = ImageFont.truetype(BOLD, 30)
f_sub = ImageFont.truetype(REG, 15)
f_col = ImageFont.truetype(BOLD, 20)

COLS = [(36, 462, "候选人查询流程"), (542, 462, "AI 助手对话流程")]

steps = [
    # (kind, title, desc)
    [
        ("start", "前端选择目标岗位", ""),
        ("proc", "GET /api/promotion/candidates", "传入 positionId、limit"),
        ("proc", "PromotionController.candidates", "接收并校验入参"),
        ("proc", "PromotionServiceImpl.candidates", "校验岗位是否存在（countPosition）"),
        ("proc", "PromotionMapper.selectCandidates", "在职 + 层级低于目标 + 绩效≥4 + 综合分"),
        ("ext", "Result<List<PromotionCandidateVO>>", "统一返回结构"),
        ("start", "前端渲染候选人排行", "按综合分降序展示"),
    ],
    [
        ("start", "前端输入问题 query", ""),
        ("proc", "POST /api/promotion/ai/chat", "携带 conversationId"),
        ("proc", "校验 Dify 是否已配置", "未配置返回明确提示"),
        ("proc", "PromotionEvidenceService.buildContext", "聚合岗位/候选/继任/技能 → 排序 → 序列化"),
        ("ext", "Dify /chat-messages（blocking）", "注入 promotion_context"),
        ("proc", "清除 <think> 推理块", "正则过滤思维链"),
        ("start", "返回 answer 与 conversationId", "支持多轮对话"),
    ],
]

BOX_H, TOP, STEP = 80, 150, 108

def fit(text, path, size, maxw, min_size=9):
    s = size
    while s > min_size:
        f = ImageFont.truetype(path, s)
        if d.textlength(text, font=f) <= maxw:
            return f
        s -= 1
    return ImageFont.truetype(path, min_size)

def draw_box(x, top, w, kind, title, desc):
    if kind == "start":
        fill, border, tc, dc = START_FILL, START_BORDER, START_TEXT, START_TEXT
    elif kind == "ext":
        fill, border, tc, dc = EXT_FILL, EXT_BORDER, EXT_TEXT, EXT_DESC
    else:
        fill, border, tc, dc = PROC_FILL, PROC_BORDER, PROC_TEXT, PROC_DESC
    cx = x + w // 2
    cy = top + BOX_H // 2
    d.rounded_rectangle([x, top, x + w, top + BOX_H], radius=12, fill=fill, outline=border, width=2)
    maxw = w - 24
    tf = fit(title, BOLD, 19, maxw)
    if desc:
        d.text((cx, cy - 10), title, font=tf, fill=tc, anchor="mm")
        df = fit(desc, REG, 14, maxw)
        d.text((cx, cy + 13), desc, font=df, fill=dc, anchor="mm")
    else:
        d.text((cx, cy), title, font=tf, fill=tc, anchor="mm")

def arrow(cx, y0, y1):
    d.line([(cx, y0), (cx, y1 - 8)], fill=ARROW, width=2)
    d.polygon([(cx, y1), (cx - 5, y1 - 9), (cx + 5, y1 - 9)], fill=ARROW)

for (x, w, label), chain in zip(COLS, steps):
    cx = x + w // 2
    d.text((cx, 104), label, font=f_col, fill=(30, 41, 59), anchor="mm")
    for i, (kind, title, desc) in enumerate(chain):
        top = TOP + i * STEP
        draw_box(x, top, w, kind, title, desc)
        if i < len(chain) - 1:
            arrow(cx, top + BOX_H + 2, top + STEP - 4)

d.text((W // 2, 38), "晋升决策模块程序流程图", font=f_title, fill=(15, 23, 42), anchor="mm")
d.text((W // 2, 70), "com.talent.promotion · 候选人查询与 AI 助手对话两条主流程",
       font=f_sub, fill=(100, 116, 139), anchor="mm")

img.save(OUT, "JPEG", quality=95, subsampling=0)
print("saved:", OUT, img.size)
