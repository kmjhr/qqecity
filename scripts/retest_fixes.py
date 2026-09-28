# -*- coding: utf-8 -*-
"""复测：3个bug修复验证 + 保函闭环 + 注册通过 + 观察期启动"""
import json, urllib.request, time

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

tu = login("testuser"); ll = login("landlord01"); en = login("entrepreneur"); bk = login("banker01")

print("=" * 100)
print("一、Bug#3 全局异常处理（缺参/类型错/路径不存在）")
print("=" * 100)
s, j = call("GET", "/v1/consumption/guide/pay-before-reminder", tu)  # 缺 amount
check("缺参→1001参数错误", j.get("code") == 1001, f"code={j.get('code')} msg={j.get('message')}")
s, j = call("GET", "/v1/policy/portal", tu)  # {id} 类型错
check("路径参数类型错→1001", j.get("code") == 1001, f"code={j.get('code')} msg={j.get('message')}")
s, j = call("GET", "/v1/nonexistent-path-xyz", tu)
check("不存在的路径→1001(非5000)", j.get("code") == 1001, f"code={j.get('code')} msg={j.get('message')}")
s, j = call("GET", "/v1/consumption/finance-product/recommend/by-level", tu)  # 缺 riskLevel
check("缺@RequestParam→1001", j.get("code") == 1001, f"code={j.get('code')} msg={j.get('message')}")
s, j = call("POST", "/v1/consumption/risk-assessment/submit", tu, {"answers": [{"questionId": 1}]})  # JSON结构错
check("JSON结构错→1001", j.get("code") == 1001, f"code={j.get('code')} msg={j.get('message')}")

print()
print("=" * 100)
print("二、Bug#1 管理端新增预警")
print("=" * 100)
s, j = call("POST", "/v1/admin/safety/alerts", bk, {"title": "QA修复验证预警", "alertLevel": "HIGH", "summary": "修复后新增", "source": "系统", "region": "全国", "status": 1})
check("管理端新增预警-成功", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} id={(j.get('data') or {}).get('id') if j.get('code')==0 else ''}")
aid = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
if aid:
    s, j = call("GET", "/v1/admin/safety/alerts", bk)
    exists = any(x.get("id") == aid for x in j.get("data", []))
    check("预警已入库并可见", exists, f"id={aid}")
    s, j = call("PUT", f"/v1/admin/safety/alerts/{aid}/status?status=0", bk)
    check("预警下架", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
    s, j = call("DELETE", f"/v1/admin/safety/alerts/{aid}", bk)
    check("预警删除", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
# 用户端预警列表能看到新增的
s, j = call("GET", "/v1/safety/alerts", tu)
check("用户端预警列表正常", j.get("code") == 0, f"code={j.get('code')} count={len(j.get('data',[]))}")

print()
print("=" * 100)
print("三、Bug#2 B类观察期幂等启动（entrepreneur 已有额度）")
print("=" * 100)
s, j = call("GET", "/v1/loan/observation", en)
print("修复前observation:", json.dumps(j.get("data"), ensure_ascii=False)[:120])
s, j = call("POST", "/v1/loan/precheck", en, {"crowdType": "在校生", "businessPlan": "校园文创店创业计划书", "purpose": "进货采购", "applyAmount": 12000})
check("预审通过", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
app_id = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
s, j = call("GET", "/v1/loan/observation", en)
obs = (j.get("data") or {}).get("observationStatus") if j.get("code") == 0 else None
check("观察期已幂等启动OBSERVING", obs == "OBSERVING", f"observationStatus={obs}")
if app_id:
    s, j = call("POST", "/v1/loan/observation/advance", en)
    check("观察期可推进1月", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")

print()
print("=" * 100)
print("四、保函缴费开立闭环（payPassword）")
print("=" * 100)
s, j = call("POST", "/v1/pay/wallet/password?password=123456", tu)
check("设置支付密码", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
body = {"landlordName": "刘房东", "landlordPhone": "13900000003", "landlordIdCard": "440100197501010011",
        "houseTitle": "海珠区测试公寓", "province": "广东", "city": "广州", "district": "海珠区",
        "address": "测试路5号505", "houseType": "APARTMENT", "area": 50, "roomCount": 1,
        "monthlyRent": 3200, "depositAmount": 3200, "rentStartDate": "2027-01-01", "rentEndDate": "2027-12-31",
        "payMethod": "MONTHLY", "contractTerms": "测试条款"}
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
        s, j = call("POST", f"/v1/pay/orders/{order_no}/pay", tu, {"payMethod": "CARD", "payPassword": "123456"})
        check("订单支付成功", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
        s, j = call("GET", f"/v1/guarantee/{gid}", tu)
        check("保函-APPROVED已开立", (j.get("data") or {}).get("applyStatus") == "APPROVED" if j.get("code") == 0 else False,
              f"applyStatus={(j.get('data') or {}).get('applyStatus') if j.get('code')==0 else j.get('message')}")

print()
print("=" * 100)
print("五、注册 GRADUATE 独立数据通过")
print("=" * 100)
s, j = call("GET", "/v1/auth/schools", tu)
sname = j.get("data", [])[0].get("schoolName")
body = {"username": "qa_g1", "password": "123456", "nickname": "毕业生", "phone": "13811112222",
        "userType": "GRADUATE", "realName": "测试毕业生", "idCard": "440100200201018888",
        "school": sname, "educationLevel": "UNDERGRADUATE", "graduationDate": "2025-06-30",
        "verifyType": "GRAD_CERT", "studentNo": "BY20250001"}
s, j = call("POST", "/v1/auth/register", None, body)
check("GRADUATE+毕业证2年内-通过", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = call("POST", "/v1/auth/login", None, {"username": "qa_g1", "password": "123456"})
check("新毕业生可登录", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")

print()
print("=" * 100)
print("结果：PASS=%d FAIL=%d" % (len(PASS), len(FAIL)))
for f in FAIL: print("  FAILED:", f)
print("=" * 100)
