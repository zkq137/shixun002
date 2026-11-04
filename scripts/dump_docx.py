from docx import Document
import sys

path = r"D:\Downloads\最终PPT.docx"
d = Document(path)
print("=== PARAGRAPHS ===")
for i, p in enumerate(d.paragraphs):
    t = p.text.strip()
    if t:
        print(f"[{i}] ({p.style.name}) {t}")

print("\n=== TABLES ===")
for ti, tb in enumerate(d.tables):
    print(f"--- table {ti} ({len(tb.rows)}x{len(tb.columns)}) ---")
    for r in tb.rows:
        print(" | ".join(c.text.strip().replace("\n", " / ") for c in r.cells))

print("\n=== INLINE SHAPES ===")
for i, sh in enumerate(d.inline_shapes):
    print(i, sh.type, sh.width, sh.height)
