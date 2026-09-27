# -*- coding: utf-8 -*-
"""全库 mojibake 扫描 + 修复脚本 v2
判定：值含 mojibake 特征字符（Latin-1 字母区 + cp1252 特有区，不含·¥等合法符号）→
      cp1252/latin-1 encode 还原字节 → utf-8 decode 成功且含 CJK → 确认为 mojibake。
      纯 mojibake（值无 CJK）→ 整体修复；混合（值含 CJK）→ 报告不修。
修复 SQL 用 HEX 写入，彻底避开引号/编码问题。
用法：python _fix_mojibake.py scan | fix
"""
import io, os, re, subprocess, sys

DB = 'qingqi'
MYSQL = ['docker', 'exec', '-i', 'qqecity-mysql', 'mysql', '-uroot', '-p123456', DB,
         '--default-character-set=utf8mb4', '-N']

# cp1252/latin-1 高半区字母及常见 mojibake 特征（正常中文业务数据不应出现）
MOJI = set()
for lo in range(0xC0, 0x100):
    MOJI.add(chr(lo))
for lo in range(0xA0, 0xC0):
    if lo in (0xB7, 0xA5, 0xA9, 0xAE, 0xB0, 0xB1, 0xB4, 0xBA, 0xBB, 0xBF):
        continue  # 合法符号：· ¥ © ® ° ± ´ º » ¿
    MOJI.add(chr(lo))
MOJI.update(['\u2013', '\u2014', '\u2018', '\u2019', '\u201A', '\u201C', '\u201D',
             '\u201E', '\u2020', '\u2021', '\u2022', '\u2026', '\u2030', '\u2039',
             '\u203A', '\u20AC', '\u2122', '\u02C6', '\u02DC'])


# cp1252 特有映射（0x80-0x9F 区），未定义字节在解码时落为对应控制字符码点（低字节直取）
CP1252_MAP = {
    0x20AC: 0x80, 0x201A: 0x82, 0x0192: 0x83, 0x201E: 0x84, 0x2026: 0x85,
    0x2020: 0x86, 0x2021: 0x87, 0x02C6: 0x88, 0x2030: 0x89, 0x0160: 0x8A,
    0x2039: 0x8B, 0x0152: 0x8C, 0x017D: 0x8E, 0x2018: 0x91, 0x2019: 0x92,
    0x201C: 0x93, 0x201D: 0x94, 0x2022: 0x95, 0x2013: 0x96, 0x2014: 0x97,
    0x02DC: 0x98, 0x2122: 0x99, 0x0161: 0x9A, 0x203A: 0x9B, 0x0153: 0x9C,
    0x017E: 0x9E, 0x0178: 0x9F,
}


def to_bytes(s):
    out = bytearray()
    for ch in s:
        o = ord(ch)
        if o <= 0xFF:
            out.append(o)
        elif o in CP1252_MAP:
            out.append(CP1252_MAP[o])
        else:
            return None
    return bytes(out)


def has_cjk(s):
    return any(0x4E00 <= ord(ch) <= 0x9FFF for ch in s)


def run_mysql(args, sql):
    p = subprocess.run(MYSQL + args, input=sql.encode('utf-8'), capture_output=True)
    return p.stdout.decode('utf-8', errors='replace')


def get_char_columns():
    sql = ("SELECT TABLE_NAME, COLUMN_NAME, COLUMN_KEY FROM information_schema.COLUMNS "
           "WHERE TABLE_SCHEMA='" + DB + "' AND DATA_TYPE IN "
           "('char','varchar','text','tinytext','mediumtext','longtext') "
           "ORDER BY TABLE_NAME, ORDINAL_POSITION;")
    out = run_mysql([], sql)
    tables = {}
    for line in out.splitlines():
        parts = line.split('\t')
        if len(parts) == 3:
            t, c, k = parts
            tables.setdefault(t, []).append((c, k))
    return tables


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else 'scan'
    tables = get_char_columns()
    print('tables=%d cols=%d' % (len(tables), sum(len(v) for v in tables.values())))

    fix_sql, hit_rows, mixed_rows = [], [], []

    for t in sorted(tables):
        pk = next((c for c, k in tables[t] if k == 'PRI'), 'id')
        for c, k in tables[t]:
            if k == 'PRI':
                continue
            sel = 'SELECT `%s` FROM `%s` WHERE `%s` IS NOT NULL;' % (c, t, c)
            pks = 'SELECT `%s` FROM `%s` WHERE `%s` IS NOT NULL;' % (pk, t, c)
            vals = run_mysql([], sel).splitlines()
            ids = run_mysql([], pks).splitlines()
            for i, v in enumerate(vals):
                if not v or not any(ch in MOJI for ch in v):
                    continue
                b = to_bytes(v)
                if b is None:
                    continue
                try:
                    fixed = b.decode('utf-8')
                except UnicodeDecodeError:
                    continue
                if '\ufffd' in fixed or not has_cjk(fixed):
                    continue
                rid = ids[i].strip() if i < len(ids) else '?'
                if not has_cjk(v):
                    hit_rows.append((t, c, rid, v, fixed))
                    hexv = fixed.encode('utf-8').hex()
                    fix_sql.append(
                        "UPDATE `%s` SET `%s` = CONVERT(0x%s USING utf8mb4) WHERE `%s` = '%s';"
                        % (t, c, hexv, pk, rid.replace("'", "\\'")))
                else:
                    mixed_rows.append((t, c, rid, v, fixed))

    print('pure-mojibake rows: %d' % len(hit_rows))
    for t, c, rid, v, f in hit_rows[:999]:
        print('  %s.%s id=%s  %s  ->  %s' % (t, c, rid, v[:50], f[:50]))
    print('mixed rows (skip): %d' % len(mixed_rows))
    for t, c, rid, v, f in mixed_rows[:15]:
        print('  MIXED %s.%s id=%s : %s' % (t, c, rid, v[:60]))

    if mode == 'fix' and fix_sql:
        print('executing %d UPDATEs ...' % len(fix_sql))
        p = subprocess.run(MYSQL, input='SET NAMES utf8mb4;\n' + '\n'.join(fix_sql),
                           capture_output=True, text=True, encoding='utf-8', errors='replace')
        print('rc=%d' % p.returncode)
        if p.stderr.strip():
            print('stderr:', p.stderr.strip()[:800])
        print('fixed rows: %d' % len(fix_sql))


if __name__ == '__main__':
    main()
