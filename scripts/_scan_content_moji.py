# -*- coding: utf-8 -*-
"""内容级 mojibake 扫描：找出文件内容本身含 mojibake 字符串的文件（编码合法但内容错误）。"""
import os, re, sys

ROOT = r'D:\codex\codex-data\qingqi-ecity'
SKIP_DIRS = {'.git', 'node_modules', 'target', 'dist', '.idea', '.vite', 'coverage'}

# mojibake 特征：Latin-1 字母区 + cp1252 区字符（正常中文业务文本几乎不含）
MOJI = set(chr(x) for x in list(range(0xC0, 0x100)) + [0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA6, 0xA7, 0xA8,
    0xAA, 0xAC, 0xAD, 0xB2, 0xB3, 0xB5, 0xB6, 0xB8, 0xB9, 0xBC, 0xBD, 0xBE])
MOJI.update(['\u2013', '\u2014', '\u2018', '\u2019', '\u201A', '\u201C', '\u201D', '\u201E',
             '\u2020', '\u2021', '\u2022', '\u2026', '\u2030', '\u2039', '\u203A', '\u20AC',
             '\u2122', '\u02C6', '\u02DC', '\u0152', '\u0153', '\u0160', '\u0161', '\u0178',
             '\u017D', '\u017E', '\u0192'])

def is_mojibake_char(ch):
    return ch in MOJI

def looks_mojibake(text):
    """连续出现 2 个以上 mojibake 特征字符，或 1 个特征字符与中文相邻"""
    count = 0
    for ch in text:
        if is_mojibake_char(ch):
            count += 1
            if count >= 3:
                return True
        else:
            count = 0
    return False

hits = []
total = 0
for dirpath, dirnames, filenames in os.walk(ROOT):
    dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
    for fn in filenames:
        ext = os.path.splitext(fn)[1].lower()
        if ext in ('.png', '.jpg', '.jpeg', '.gif', '.ico', '.woff', '.woff2', '.ttf', '.map', '.jar', '.class', '.pyc'):
            continue
        p = os.path.join(dirpath, fn)
        try:
            raw = open(p, 'rb').read()
        except Exception:
            continue
        # 只处理文本类
        if b'\x00' in raw[:8000]:
            continue
        try:
            text = raw.decode('utf-8')
        except UnicodeDecodeError:
            continue
        total += 1
        # 提取含 mojibake 的行/片段
        for m in re.finditer(r'[^\n]{0,40}%s[^\n]{0,40}' % ('[' + ''.join(re.escape(c) for c in MOJI) + ']',), text):
            seg = m.group(0)
            if looks_mojibake(seg):
                rel = os.path.relpath(p, ROOT)
                hits.append((rel, seg.strip()[:90]))
                break

print('total text files: %d' % total)
print('content-mojibake hits: %d' % len(hits))
for rel, seg in hits:
    print('%-70s | %s' % (rel, seg))
