# -*- coding: utf-8 -*-
"""注册全场景修正（educationLevel=UNDERGRADUATE）+ 支付密码 + 预警复测"""
import json, urllib.request

BASE = "http://localhost:8080/api"
def call(method, path, token=None, body=None, timeout=60):
    req = urllib.request.Request(BASE + path, data=json.dumps(body, ensure_ascii=False).encode() if body is not None else None, method=method)
    req.add_header("Content-Type", "application/json")
    if token: req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        try: return e.code, json.loads(e.read().decode("utf-8", "replace"))
        except Exception: return e.code, {}
    except Exception as e:
        return "ERR", {"err": str(e)}

def login(u, p="123456"):
    s, j = call("POST", "/v1/auth/login", None, {"username": u, "password": p})
    return j["data"]["accessToken"] if j.get("code") == 0 else None

PASS, FAIL = [], []
def check(name, cond, detail=""):
    (PASS if cond else FAIL).append(name)
    print(f"[{'PASS' if cond else 'FAIL'}] {name} {detail}")

tu = login("testuser"); ll = login("landlord01"); bk = login("banker01")

print("=" * 100)
print("一、注册全场景（educationLevel=UNDERGRADUATE）")
print("=" * 100)
s, j = call("GET", "/v1/auth/schools", tu)
sname = j.get("data", [])[0].get("schoolName")

def reg(user, **kw):
    body = {"username": user, "password": "123456", "nickname": "测试", "phone": "13800007777",
            "userType": "STUDENT", "realName": "测试员", "idCard": "440100200601017777",
            "school": sname, "educationLevel": "UNDERGRADUATE", "graduationDate": "2027-06-30",
            "verifyType": "XUE_XIN_WANG", "studentNo": "XW20260002"}
    body.update(kw)
    return call("POST", "/v1/auth/register", None, body)

s, j = reg("qa_s1")
check("STUDENT+学信网+白名单学校-通过", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_s2", verifyType="GRAD_CERT")
check("STUDENT误用毕业证-拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_s3", school="不存在大学XYZ")
check("非白名单学校-拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_s4", userType="GRADUATE", graduationDate="2025-06-30", verifyType="STUDENT_CARD")
check("GRADUATE误用学生证-拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_s5", userType="GRADUATE", graduationDate="2025-06-30", verifyType="GRAD_CERT")
check("GRADUATE+毕业证2年内-通过", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_s6", userType="GRADUATE", graduationDate="2020-06-30", verifyType="GRAD_CERT")
check("毕业超2年-拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_s7", userType="ENTREPRENEUR")
check("青年创业者-通过", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_s1")
check("重复用户名-拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_s8", verifyType=None, studentNo=None)
check("缺核验材料-拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
# 新注册用户登录验证
s, j = call("POST", "/v1/auth/login", None, {"username": "qa_s1", "password": "123456"})
check("新注册用户可登录", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")

print()
print("=" * 100)
print("二、支付密码 + 保函缴费开立闭环")
print("=" * 100)
s, j = call("POST", "/v1/pay/wallet/password?password=123456", tu)
check("设置支付密码", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
body = {
    "landlordName": "陈房东", "landlordPhone": "13900000003", "landlordIdCard": "440100197701010011",
    "houseTitle": "越秀区测试公寓", "province": "广东", "city": "广州", "district": "越秀区",
    "address": "测试路4号404", "houseType": "APARTMENT", "area": 60, "roomCount": 2,
    "monthlyRent": 4000, "depositAmount": 4000,
    "rentStartDate": "2026-12-01", "rentEndDate": "2027-11-30",
    "payMethod": "MONTHLY", "contractTerms": "测试条款"
}
s, j = call("POST", "/v1/guarantee/apply", tu, body)
gid = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
check("保函申请", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
if gid:
    s, j = call("PUT", f"/v1/guarantee/{gid}/landlord-confirm", ll, {"signContent": "CLICK_CONFIRM"})
    check("房东确认", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
    s, j = call("POST", f"/v1/guarantee/{gid}/pay", tu)
    order_no = (j.get("data") or {}).get("orderNo") if j.get("code") == 0 else None
    check("创建缴费订单", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
    if order_no:
        s, j = call("POST", f"/v1/pay/orders/{order_no}/pay", tu, {"payMethod": "CARD"})
        check("订单支付(CARD)", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
        s, j = call("GET", f"/v1/guarantee/{gid}", tu)
        check("保函-APPROVED已开立", (j.get("data") or {}).get("applyStatus") == "APPROVED" if j.get("code") == 0 else False,
              f"applyStatus={(j.get('data') or {}).get('applyStatus') if j.get('code')==0 else j.get('message')}")
        gno = (j.get("data") or {}).get("guaranteeId") if j.get("code") == 0 else None
        if gno:
            s, j = call("GET", f"/v1/guarantee/guarantee/{gno}", tu)
            check("电子保函详情", j.get("code") == 0, f"status={(j.get('data') or {}).get('status') if j.get('code')==0 else ''}")

print()
print("=" * 100)
print("结果：PASS=%d FAIL=%d" % (len(PASS), len(FAIL)))
for f in FAIL: print("  FAILED:", f)
print("=" * 100)
