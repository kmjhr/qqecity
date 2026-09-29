# -*- coding: utf-8 -*-
"""青启e城 深测第三轮：注册白名单全场景/保函支付/充值还款/情景提交/管理端修正"""
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

tu = login("testuser"); ll = login("landlord01"); en = login("entrepreneur")
bk = login("banker01"); ad = login("admin")

print("=" * 100)
print("一、注册白名单全场景（正确字段 userType/verifyType/school/educationLevel/graduationDate/studentNo）")
print("=" * 100)
s, j = call("GET", "/v1/auth/schools", tu)
schools = j.get("data", [])
sname = schools[0].get("schoolName")
sname2 = schools[1].get("schoolName") if len(schools) > 1 else sname
print("白名单学校样例:", sname, "/", sname2)

def reg(user, **kw):
    body = {"username": user, "password": "123456", "nickname": "测试", "phone": "13800009999",
            "userType": "STUDENT", "realName": "测试员", "idCard": "440100200601019999",
            "school": sname, "educationLevel": "本科", "graduationDate": "2027-06-30",
            "verifyType": "XUE_XIN_WANG", "studentNo": "XW20260001"}
    body.update(kw)
    return call("POST", "/v1/auth/register", None, body)

s, j = reg("qa_r1")
check("注册-STUDENT+学信网+白名单学校", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_r2", verifyType="GRAD_CERT")
check("注册-STUDENT误用毕业证拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_r3", school="不存在大学XYZ")
check("注册-非白名单学校拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_r4", userType="GRADUATE", graduationDate="2025-06-30", verifyType="STUDENT_CARD")
check("注册-GRADUATE误用学生证拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_r5", userType="GRADUATE", graduationDate="2025-06-30", verifyType="GRAD_CERT")
check("注册-GRADUATE+毕业证2年内通过", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_r6", userType="GRADUATE", graduationDate="2020-06-30", verifyType="GRAD_CERT")
check("注册-毕业超2年拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_r7", userType="ENTREPRENEUR")
check("注册-青年创业者通过", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = reg("qa_r1")
check("注册-重复用户名拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
# 缺核验材料
s, j = reg("qa_r8", verifyType=None, studentNo=None)
check("注册-缺核验材料拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")

print()
print("=" * 100)
print("二、保函全流程（applyStatus 断言 + payMethod=CARD）")
print("=" * 100)
body = {
    "landlordName": "王房东", "landlordPhone": "13900000003", "landlordIdCard": "440100197901010011",
    "houseTitle": "白云区测试公寓", "province": "广东", "city": "广州", "district": "白云区",
    "address": "测试路3号303", "houseType": "APARTMENT", "area": 55, "roomCount": 2,
    "monthlyRent": 3500, "depositAmount": 3500,
    "rentStartDate": "2026-11-01", "rentEndDate": "2027-10-31",
    "payMethod": "MONTHLY", "contractTerms": "测试条款"
}
s, j = call("POST", "/v1/guarantee/apply", tu, body)
check("保函申请-提交", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
gid = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
check("保函-初始applyStatus=SUBMITTED", (j.get("data") or {}).get("applyStatus") == "SUBMITTED" if j.get("code") == 0 else False,
      f"applyStatus={(j.get('data') or {}).get('applyStatus') if j.get('code')==0 else ''}")
if gid:
    s, j = call("PUT", f"/v1/guarantee/{gid}/landlord-confirm", ll, {"signContent": "CLICK_CONFIRM"})
    check("房东确认", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} applyStatus={(j.get('data') or {}).get('applyStatus') if j.get('code')==0 else ''}")
    s, j = call("POST", f"/v1/guarantee/{gid}/pay", tu)
    check("创建缴费订单", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
    order_no = (j.get("data") or {}).get("orderNo") if j.get("code") == 0 else None
    if order_no:
        s, j = call("POST", f"/v1/pay/orders/{order_no}/pay", tu, {"payMethod": "CARD"})
        check("订单支付(CARD)", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
    s, j = call("GET", f"/v1/guarantee/{gid}", tu)
    check("缴费后-APPROVED已开立", (j.get("data") or {}).get("applyStatus") == "APPROVED" if j.get("code") == 0 else False,
          f"applyStatus={(j.get('data') or {}).get('applyStatus') if j.get('code')==0 else j.get('message')}")
    gno = (j.get("data") or {}).get("guaranteeId") if j.get("code") == 0 else None
    if gno:
        s, j = call("GET", f"/v1/guarantee/guarantee/{gno}", tu)
        check("电子保函详情", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} status={(j.get('data') or {}).get('status') if j.get('code')==0 else ''}")

print()
print("=" * 100)
print("三、充值(CARD) + 循环贷还款闭环")
print("=" * 100)
s, j = call("POST", "/v1/pay/wallet/recharge", tu, {"amount": 50000, "method": "CARD"})
check("钱包充值(CARD)", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} balance={(j.get('data') or {}).get('balance') if j.get('code')==0 else ''}")
s, j = call("POST", "/v1/loan/repay", tu, {"amount": 8000})
check("循环贷还款8000", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
s, j = call("GET", "/v1/loan/credit", tu)
avail = [c for c in j.get("data", []) if c.get("creditType") == "A_TYPE"]
check("还款后A额度恢复5万", avail and avail[0].get("availableLimit") == 50000.0 if j.get("code") == 0 else False,
      f"available={avail[0].get('availableLimit') if avail else '?'}")

print()
print("=" * 100)
print("四、情景教学提交（answers Map key→A/B/C/D）")
print("=" * 100)
s, j = call("GET", "/v1/safety/scenario/list", tu)
scenarios = j.get("data", [])
if scenarios:
    sc = scenarios[0]
    s, j = call("GET", f"/v1/safety/scenario/{sc.get('id')}", tu)
    qs = j.get("data", {}).get("questions") or []
    if qs:
        ans = {}
        for q in qs:
            qid = q.get("questionNo") or q.get("id")
            opts = q.get("options") or []
            if opts and qid is not None:
                ans[str(qid)] = opts[0].get("key") or "A"
        s, j = call("POST", f"/v1/safety/scenario/{sc.get('id')}/submit", tu, {"answers": ans})
        check("情景提交评分", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
    else:
        check("情景提交", False, "无问题数据")
else:
    check("情景列表", False, "无情景")

print()
print("=" * 100)
print("五、管理端修正（预警 alertLevel/summary、用户 role/status）")
print("=" * 100)
s, j = call("POST", "/v1/admin/safety/alerts", bk, {"title": "QA预警", "alertLevel": "MEDIUM", "summary": "QA测试摘要", "source": "系统", "region": "全国", "status": 1})
check("管理端-新增预警", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
aid = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
if aid:
    s, j = call("PUT", f"/v1/admin/safety/alerts/{aid}/status?status=0", bk)
    check("管理端-预警下架", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
    s, j = call("DELETE", f"/v1/admin/safety/alerts/{aid}", bk)
    check("管理端-删除预警", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = call("POST", "/admin/v1/user", ad, {"username": "qa_sys02", "password": "123456", "nickname": "QA用户", "role": "USER", "status": 1})
check("管理端-新增用户", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
uid = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
if uid:
    s, j = call("DELETE", f"/admin/v1/user/{uid}", ad)
    check("管理端-删除用户", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
# 风险预警处理 handleNote
s, j = call("GET", "/v1/admin/risk-warnings", bk)
warns = (j.get("data") or {}).get("records") or []
if warns:
    wid = warns[0].get("id")
    s, j = call("PUT", f"/v1/admin/risk-warnings/{wid}/handle", bk, {"handleNote": "QA已处理", "handleStatus": "HANDLED"})
    check("管理端-风险预警处理", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")

print()
print("=" * 100)
print("结果：PASS=%d FAIL=%d" % (len(PASS), len(FAIL)))
for f in FAIL: print("  FAILED:", f)
print("=" * 100)
