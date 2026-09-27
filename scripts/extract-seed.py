# -*- coding: utf-8 -*-
# 提取 data.sql 中 biz_anti_fraud_content 与 biz_merchant 两条 INSERT 语句（含 SCENARIO_SIM 情景与 PENDING 商户），
# 生成补灌脚本，用于把旧数据卷缺的种子补进运行库（INSERT IGNORE 幂等）。
import re, io, sys

src = r'D:\codex\codex-data\qingqi-ecity\backend\sql\data.sql'
out = r'D:\codex\codex-data\qingqi-ecity\scripts\fix-seed.sql'

with io.open(src, 'r', encoding='utf-8') as f:
    text = f.read()

def extract(stmt_start, until=';\n'):
    idx = text.find(stmt_start)
    if idx < 0:
        raise RuntimeError('not found: ' + stmt_start)
    end = text.find(until, idx)
    return text[idx:end+1]

ins1 = extract('INSERT IGNORE INTO `biz_anti_fraud_content`')
ins2 = extract('INSERT IGNORE INTO `biz_merchant`')

with io.open(out, 'w', encoding='utf-8') as f:
    f.write('-- 补灌运行库缺失种子（幂等）：SCENARIO_SIM 情景 3 条 + PENDING 商户 1 条\n')
    f.write('SET NAMES utf8mb4;\n')
    f.write(ins1 + '\n')
    f.write(ins2 + '\n')

print('wrote', out)
print('ins1 len', len(ins1), 'ins2 len', len(ins2))
