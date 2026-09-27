import os, io, sys
root = r'D:\codex\codex-data\qingqi-ecity'
exts = {'.java','.sql','.js','.vue','.md','.ps1','.py','.xml','.yml','.yaml','.json','.css','.html','.ts','.txt','.properties','.env.example'}
issues = []
mojibake_files = []
total = 0
for dirpath, dirnames, filenames in os.walk(root):
    dirnames[:] = [d for d in dirnames if d not in {'.git','node_modules','target','dist','.idea'}]
    for f in filenames:
        if not f.endswith(tuple(exts)):
            continue
        p = os.path.join(dirpath, f)
        total += 1
        try:
            raw = open(p,'rb').read()
        except Exception:
            continue
        if not raw:
            continue
        # 1) UTF-8 合法性（忽略 BOM）
        try:
            raw.decode('utf-8')
        except UnicodeDecodeError:
            issues.append((p, 'NOT_UTF8'))
            continue
        # 2) mojibake 特征：UTF-8 被 Latin-1/GBK 双重编码（å¯¹ è¯ æ¼ 等特征字节序列）
        s = raw.decode('utf-8')
        # 常见 mojibake 标记字符
        markers = ['Ã©','Ã¨','Ã¤','Ã¦','å¯¹','è¯','æ¼','éª','ç½','ä¸','é—','æ—','åˆ','ï¼','â€','Â·','æ¨¡','æ‹','ç¼','é»']
        hits = [m for m in markers if m in s]
        if hits:
            mojibake_files.append((p, hits[:4]))
print('TOTAL_TEXT_FILES:', total)
print('== NOT UTF-8 files ==')
for p, why in issues:
    print(' !', p, why)
print('== MOJIBAKE suspect files ==')
for p, h in mojibake_files:
    print(' ?', p, h)