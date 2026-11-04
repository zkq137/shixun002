# -*- coding: utf-8 -*-
"""生成「平台系统架构图」JPG（四层：用户层 / 接入层 / 服务层 / 数据与能力层）。"""
import os
from PIL import Image, ImageDraw, ImageFont

OUT = r"D:\202609shixun\shixun002\平台系统架构图.jpg"
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

W, H = 1400, 730
BG = (255, 255, 255)
LINE = (148, 163, 184)

USER_F, USER_L = (241, 245, 249), (100, 116, 139)
GATE_F, GATE_L = (254, 243, 199), (245, 158, 11)
SVC_F, SVC_L = (219, 234, 254), (59, 130, 246)
DATA_F, DATA_L = (220, 252, 231), (34, 197, 94)

img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

f_tag = ImageFont.truetype(BOLD, 17)
f_name = ImageFont.truetype(BOLD, 21)
f_desc = ImageFont.truetype(REG, 14)
f_svc = ImageFont.truetype(BOLD, 19)
f_svcd = ImageFont.truetype(REG, 13.5)

def fit(text, path, size, maxw, min_size=9):
    s = size
    while s > min_size:
        f = ImageFont.truetype(path, s)
        if d.textlength(text, font=f) <= maxw:
            return f
        s -= 1
    return ImageFont.truetype(path, min_size)

def box(x, y, w, h, fill, border):
    d.rounded_rectangle([x, y, x + w, y + h], radius=12, fill=fill, outline=border, width=2)

def two_line(cx, cy, name, desc, fn, fd, tc, dc, maxw):
    d.text((cx, cy - 11), name, font=fit(name, BOLD, fn, maxw), fill=tc, anchor="mm")
    if desc:
        d.text((cx, cy + 16), desc, font=fit(desc, REG, fd, maxw), fill=dc, anchor="mm")
def arrow_down(x, y0, y1, color=LINE):
    d.line([(x, y0), (x, y1 - 9)], fill=color, width=2)
    d.polygon([(x, y1), (x - 6, y1 - 11), (x + 6, y1 - 11)], fill=color)

def tag(y, text):
    d.text((42, y), text, font=f_tag, fill=(100, 116, 139), anchor="lm")

BX, BW = 170, 1180

# ---- 用户层 ----
tag(102, "用户层")
box(BX, 60, BW, 85, USER_F, USER_L)
two_line(BX + BW // 2, 60 + 42, "浏览器 · Vue 3 + Element Plus 前端",
         "员工档案 · 梯队规划 · 智能培训 · 晋升决策 · 数据分析 · 技能体系", 21, 14,
         (30, 41, 59), (71, 85, 105), BW - 40)

# ---- 接入层 ----
tag(228, "接入层")
box(BX, 185, BW, 85, GATE_F, GATE_L)
two_line(BX + BW // 2, 185 + 42, "API 网关 talent-gateway : 9090",
         "统一路由 /api/** · Sentinel 限流熔断 · 前端只对接一个入口", 21, 14,
         (120, 53, 15), (146, 64, 14), BW - 40)

arrow_down(BX + BW // 2, 148, 182)

# ---- 服务层 ----
tag(392, "服务层")
svcs = [
    ("员工档案 8081", "档案 / 绩效 / 潜力 / 技能"),
    ("梯队规划 8082", "岗位 / 继任 / 风险预警"),
    ("智能培训 8083", "课程 / 学习路径 / 记录"),
    ("晋升决策 8084", "候选筛选 / 评分 / AI"),
    ("数据分析 8085", "看板 / 模型 / 报表"),
]
sw, gap = 215, 18
start = BX
bus_y = 305
for i, (name, desc) in enumerate(svcs):
    x = start + i * (sw + gap)
    cx = x + sw // 2
    box(x, 330, sw, 120, SVC_F, SVC_L)
    two_line(cx, 330 + 60, name, desc, 19, 13.5, (30, 58, 138), (37, 99, 235), sw - 18)
    d.line([(cx, bus_y), (cx, 327)], fill=LINE, width=2)
d.line([(start + sw // 2, bus_y), (start + 4 * (sw + gap) + sw // 2, bus_y)], fill=LINE, width=2)
d.line([(BX + BW // 2, 273), (BX + BW // 2, bus_y)], fill=LINE, width=2)
d.polygon([(BX + BW // 2, bus_y - 1), (BX + BW // 2 - 6, bus_y - 12), (BX + BW // 2 + 6, bus_y - 12)], fill=LINE)

# ---- 数据与能力层 ----
tag(607, "数据层")
bus2 = 515
d.line([(start + i * (sw + gap) + sw // 2, 450) for i in range(5)] + [(start + 4 * (sw + gap) + sw // 2, 450)],
       fill=LINE, width=2)
for i in range(5):
    cx = start + i * (sw + gap) + sw // 2
    d.line([(cx, 450), (cx, bus2)], fill=LINE, width=2)
d.line([(start + sw // 2, bus2), (start + 4 * (sw + gap) + sw // 2, bus2)], fill=LINE, width=2)

datas = [
    ("MySQL · talent_db", "13 张表：员工 / 岗位 / 绩效 / 培训 / 报表"),
    ("Nacos 2.4 · 8848", "注册中心 + 配置中心"),
    ("Dify Cloud", "5 个工作流智能体"),
]
dw, dgap = 350, 40
dstart = BX + 45
for i, (name, desc) in enumerate(datas):
    x = dstart + i * (dw + dgap)
    cx = x + dw // 2
    box(x, 558, dw, 100, DATA_F, DATA_L)
    two_line(cx, 558 + 50, name, desc, 19, 13.5, (20, 83, 45), (22, 101, 52), dw - 20)
    d.line([(cx, bus2), (cx, 555)], fill=LINE, width=2)
    d.polygon([(cx, 555), (cx - 6, 544), (cx + 6, 544)], fill=LINE)

d.text((W // 2, 700), "跨模块数据：服务间通过网关聚合 / Feign 调用（如晋升决策读取岗位画像与继任候选）",
       font=ImageFont.truetype(REG, 14), fill=(100, 116, 139), anchor="mm")

img.save(OUT, "JPEG", quality=95, subsampling=0)
print("saved", OUT, img.size)

