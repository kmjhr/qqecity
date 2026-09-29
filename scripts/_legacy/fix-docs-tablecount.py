# -*- coding: utf-8 -*-
"""修正文档中过时的表数量描述 41 -> 43（schema.sql 当前 43 张表）。"""
import io, os

ROOT = r"D:\codex\codex-data\qingqi-ecity"

REPLACES = [
    # (path, [(old, new)])
    (os.path.join(ROOT, "README.md"), [
        ("41 张数据表", "43 张数据表"),
        ("MySQL 中有 41 张表", "MySQL 中有 43 张表"),
    ]),
    (os.path.join(ROOT, "docs", "deployment", "02-project-setup.md"), [
        ("应返回 41", "应返回 43"),
    ]),
    (os.path.join(ROOT, "docs", "deployment", "05-troubleshooting.md"), [
        ("应为 41 张", "应为 43 张"),
        ("应为 41", "应为 43"),
    ]),
    (os.path.join(ROOT, "docs", "deployment", "docker-compose.md"), [
        ("建表（41 张表）", "建表（43 张表）"),
        ("41 张表", "43 张表"),
    ]),
    (os.path.join(ROOT, "docs", "deployment", "mysql-deployment.md"), [
        ("导入表结构（41 张表）", "导入表结构（43 张表）"),
        ("41 张表", "43 张表"),
    ]),
    (os.path.join(ROOT, "docs", "deployment", "README.md"), [
        ("表数量 = 41", "表数量 = 43"),
    ]),
    (os.path.join(ROOT, "docs", "common", "tech-stack.md"), [
        ("41 ", "43 "),
    ]),
]

for path, pairs in REPLACES:
    if not os.path.exists(path):
        print("SKIP missing:", path)
        continue
    with io.open(path, "r", encoding="utf-8-sig") as f:
        text = f.read()
    for old, new in pairs:
        if old in text:
            text = text.replace(old, new)
            print("OK  ", os.path.relpath(path, ROOT), ":", old, "->", new)
        else:
            print("N/A ", os.path.relpath(path, ROOT), ":", old)
    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.write(text)

print("done")
