# -*- coding: utf-8 -*-
"""清理剩余 41 表引用（README.md 目录树注释、docs/backend/architecture.md、docs/deployment/README.md）。"""
import io, os

ROOT = r"D:\codex\codex-data\qingqi-ecity"
files = [
    os.path.join(ROOT, "README.md"),
    os.path.join(ROOT, "docs", "backend", "architecture.md"),
    os.path.join(ROOT, "docs", "deployment", "README.md"),
]
for p in files:
    with io.open(p, "r", encoding="utf-8-sig") as f:
        t = f.read()
    n = t.count("41 张")
    t = t.replace("41 张", "43 张")
    with io.open(p, "w", encoding="utf-8", newline="") as f:
        f.write(t)
    print(os.path.relpath(p, ROOT), "replaced:", n)
print("done")
