# -*- coding: utf-8 -*-
# 精确替换 schema.sql 中 CYTX 四行（policy_type/target_crowd 列错位修复）
import io, sys

path = r'D:\codex\codex-data\qingqi-ecity\backend\sql\schema.sql'
with io.open(path, 'r', encoding='utf-8') as f:
    text = f.read()

old_lines = [
    "('CYTX-001', '创业担保贷款（个人最高30万）', 'ENTREPRENEUR,GRADUATE', 300000.00, '财政贴息最高3%（LPR-150BP以内部分）', '法定劳动年龄内；有具体经营项目；信用良好；参加过创业培训', '/api/v1/policy/apply/5', '市人社局创业指导科', 5),",
    "('CYTX-002', '创业担保贷款（部分地区最高50万）', 'ENTREPRENEUR', 500000.00, '财政贴息LPR-150BP以内部分', '创业担保贷款优质项目；经营满1年；带动就业5人以上；部分地区试点', '/api/v1/policy/apply/6', '市人社局创业指导科', 6),",
    "('CYTX-003', '个体工商户税费减免', 'ENTREPRENEUR', 0.00, '月销售额10万以内免征增值税', '办理个体工商户营业执照；月销售额不超过10万元；小规模纳税人', '/api/v1/policy/apply/7', '市税务局', 7),",
    "('CYTX-004', '青年创业场地补贴', 'ENTREPRENEUR,GRADUATE', 18000.00, '每年最高6000元最长3年', '毕业5年内青年；首次创办经营实体；入驻认定的创业孵化基地', '/api/v1/policy/apply/8', '市科技局', 8);",
]
new_lines = [
    "('CYTX-001', '创业担保贷款（个人最高30万）', 'ENTREPRENEUR', 'ENTREPRENEUR,GRADUATE', 300000.00, '财政贴息最高3%（LPR-150BP以内部分）', '法定劳动年龄内；有具体经营项目；信用良好；参加过创业培训', '/api/v1/policy/apply/5', '市人社局创业指导科', 5),",
    "('CYTX-002', '创业担保贷款（部分地区最高50万）', 'ENTREPRENEUR', 'ENTREPRENEUR', 500000.00, '财政贴息LPR-150BP以内部分', '创业担保贷款优质项目；经营满1年；带动就业5人以上；部分地区试点', '/api/v1/policy/apply/6', '市人社局创业指导科', 6),",
    "('CYTX-003', '个体工商户税费减免', 'ENTREPRENEUR', 'ENTREPRENEUR', 0.00, '月销售额10万以内免征增值税', '办理个体工商户营业执照；月销售额不超过10万元；小规模纳税人', '/api/v1/policy/apply/7', '市税务局', 7),",
    "('CYTX-004', '青年创业场地补贴', 'ENTREPRENEUR', 'ENTREPRENEUR,GRADUATE', 18000.00, '每年最高6000元最长3年', '毕业5年内青年；首次创办经营实体；入驻认定的创业孵化基地', '/api/v1/policy/apply/8', '市科技局', 8);",
]

count = 0
for old, new in zip(old_lines, new_lines):
    if old in text:
        text = text.replace(old, new)
        count += 1
    else:
        print('NOT FOUND:', old[:40])
        sys.exit(1)

with io.open(path, 'w', encoding='utf-8', newline='') as f:
    f.write(text)

print('replaced', count, 'lines OK')
