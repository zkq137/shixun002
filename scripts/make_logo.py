# -*- coding: utf-8 -*-
"""把校徽白底转成透明，便于放在深色封面与浅色页面上。"""
from PIL import Image

SRC = r"D:\202609shixun\shixun002\school_logo.png"
DST = r"D:\202609shixun\shixun002\.tmp\school_logo_t.png"

im = Image.open(SRC).convert("RGB")
px = im.load()
w, h = im.size
out = Image.new("RGBA", (w, h))
op = out.load()
for y in range(h):
    for x in range(w):
        r, g, b = px[x, y]
        m = min(r, g, b)
        if m < 190:
            a = 255
        else:
            a = max(0, min(255, int((255 - m) * 255 / 65)))
        op[x, y] = (r, g, b, a)
out.save(DST)
print("saved", DST, out.size)
