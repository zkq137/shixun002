# -*- coding: utf-8 -*-
"""把结构图、流程图插入实训报告：替换对应【占位】段落，加图注，并保证标题不与图分离。"""
import os
from docx import Document
from docx.shared import Cm, Pt, Emu
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

DOCX = os.environ.get("REPORT_OUT") or r"D:\202609shixun\shixun002\天津工业大学项目实训总结报告（晋升决策模块）.docx"

FIGURES = [
    ("结构图占位", r"D:\202609shixun\shixun002\晋升决策模块结构图.jpg", "图 2-1 晋升决策模块结构图"),
    ("流程图占位", r"D:\202609shixun\shixun002\晋升决策模块程序流程图.jpg", "图 2-2 晋升决策模块程序流程图"),
]

doc = Document(DOCX)

sec = doc.sections[0]
avail_emu = sec.page_width - sec.left_margin - sec.right_margin
width = Emu(min(avail_emu, int(Cm(15.6))))
print("available cm:", round(avail_emu / 360000, 2), "-> use cm:", round(width / 360000, 2))


def find_para(marker):
    for p in doc.paragraphs:
        if marker in p.text:
            return p
    return None


def borderless(shape):
    spPr = shape._inline.graphic.graphicData.pic.spPr
    for old in spPr.findall(qn("a:ln")):
        spPr.remove(old)
    ln = OxmlElement("a:ln")
    ln.append(OxmlElement("a:noFill"))
    spPr.append(ln)


for marker, img, caption in FIGURES:
    target = find_para(marker)
    if target is None:
        print("skip (marker not found):", marker)
        continue

    for r in list(target.runs):
        r._element.getparent().remove(r._element)

    pf = target.paragraph_format
    pf.alignment = WD_ALIGN_PARAGRAPH.CENTER
    pf.first_line_indent = None
    pf.line_spacing = 1.0
    pf.space_before = Pt(6)
    pf.space_after = Pt(3)

    target.add_run().add_picture(img, width=width)
    borderless(doc.inline_shapes[-1])

    # 图注
    cap = doc.add_paragraph()
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cap.paragraph_format.first_line_indent = None
    cap.paragraph_format.space_before = Pt(0)
    cap.paragraph_format.space_after = Pt(10)
    run = cap.add_run(caption)
    run.font.name = "宋体"
    run._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
    run.font.size = Pt(10.5)
    target._p.addnext(cap._p)

    # 让图前的说明段与图保持在同一页（避免标题/说明被留在上一页）
    prev = target._p.getprevious()
    if prev is not None and prev.tag.endswith("}p"):
        from docx.text.paragraph import Paragraph
        Paragraph(prev, target._parent).paragraph_format.keep_with_next = True

    print("inserted:", marker, "->", caption)

try:
    doc.save(DOCX)
    dest = DOCX
except PermissionError:
    dest = DOCX.replace(".docx", "（含图）.docx")
    doc.save(dest)
print("saved to:", dest)
print("inline shapes:", len(doc.inline_shapes))
print("placeholders left:", sum(1 for p in doc.paragraphs if "占位" in p.text))
