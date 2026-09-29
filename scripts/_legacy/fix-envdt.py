# -*- coding: utf-8 -*-
import io

p = r'D:\codex\codex-data\qingqi-ecity\admin-web\src\env.d.ts'
s = io.open(p, 'r', encoding='utf-8').read()
old = "role: 'USER' | 'ADMIN'"
new = "role: 'USER' | 'ADMIN' | 'BANK_OPERATOR' | 'LANDLORD'"
assert old in s, 'target not found'
io.open(p, 'w', encoding='utf-8', newline='').write(s.replace(old, new))
print('env.d.ts patched')
