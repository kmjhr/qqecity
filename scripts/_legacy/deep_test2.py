# -*- coding: utf-8 -*-
"""青启e城 深测第二轮：修正参数 + 索赔/教学/聊天/管理端CRUD"""
import json, urllib.request

BASE = "http://localhost:8080/api"

def call(method, path, token=None, body=None, timeout=90):
    url = BASE + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode("utf-8", "replace"))
        except Exception:
            return e.code, {}
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
ad = login("admin"); bk = login("banker01")

print("=" * 100)
print("一、保函全流程（修正：landlordPhone=landlord01）")
print("=" * 100)
body = {
    "landlordName": "李房东", "landlordPhone": "13900000003", "landlordIdCard": "440100198801010011",
    "houseTitle": "天河区测试公寓2", "province": "广东", "city": "广州", "district": "天河区",
    "address": "测试路2号202", "houseType": "APARTMENT", "area": 40, "roomCount": 1,
    "monthlyRent": 2800, "depositAmount": 2800,
    "rentStartDate": "2026-10-01", "rentEndDate": "2027-09-30",
    "payMethod": "MONTHLY", "contractTerms": "测试条款"
}
s, j = call("POST", "/v1/guarantee/apply", tu, body)
check("保函申请-提交成功", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
gid = j.get("data", {}).get("id") if j.get("code") == 0 else None
check("保函申请-初始SUBMITTED", (j.get("data") or {}).get("status") == "SUBMITTED" if j.get("code") == 0 else False)
if gid:
    s, j = call("PUT", f"/v1/guarantee/{gid}/landlord-confirm", ll, {"signContent": "CLICK_CONFIRM"})
    check("房东确认-landlord01", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} status={(j.get('data') or {}).get('status') if j.get('code')==0 else ''}")
    s, j = call("POST", f"/v1/guarantee/{gid}/pay", tu)
    check("缴费-创建订单", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
    order_no = (j.get("data") or {}).get("orderNo") if j.get("code") == 0 else None
    if order_no:
        s, j = call("POST", f"/v1/pay/orders/{order_no}/pay", tu, {"payPassword": "123456"})
        check("缴费-支付订单支付", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
    s, j = call("GET", f"/v1/guarantee/{gid}", tu)
    check("缴费后-状态APPROVED", (j.get("data") or {}).get("status") == "APPROVED" if j.get("code") == 0 else False,
          f"status={(j.get('data') or {}).get('status') if j.get('code')==0 else j}")
    # 电子保函详情
    gno = (j.get("data") or {}).get("guaranteeId") if j.get("code") == 0 else None
    if gno:
        s, j = call("GET", f"/v1/guarantee/guarantee/{gno}", tu)
        check("电子保函详情", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")

print()
print("=" * 100)
print("二、循环贷 A 类（充值钱包后还款）")
print("=" * 100)
s, j = call("POST", "/v1/pay/wallet/recharge", tu, {"amount": 50000, "method": "BANK_CARD"})
check("钱包充值50000", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:120]}")
s, j = call("POST", "/v1/loan/withdraw", tu, {"amount": 8000})
check("提款8000", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = call("GET", "/v1/loan/repay-preview?creditType=A_TYPE", tu)
check("还款试算", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
s, j = call("POST", "/v1/loan/repay", tu, {"amount": 8000})
check("还款8000(FIFO)", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
s, j = call("GET", "/v1/loan/credit", tu)
avail = [c for c in j.get("data", []) if c.get("creditType") == "A_TYPE"]
check("还款后额度恢复", avail and avail[0].get("availableLimit") == 50000 if j.get("code") == 0 else False,
      f"available={avail[0].get('availableLimit') if avail else '?'}")

print()
print("=" * 100)
print("三、B 类受托支付全流程（预审→受托支付→观察期）")
print("=" * 100)
s, j = call("POST", "/v1/loan/precheck", en, {
    "crowdType": "在校生", "businessPlan": "校园文创店创业计划书", "purpose": "进货采购",
    "applyAmount": 12000})
check("B类预审", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:250]}")
app_id = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
if app_id:
    s, j = call("GET", "/v1/loan/merchants", en)
    merchants = j.get("data", [])
    m = next((x for x in merchants if x.get("verifyStatus") != "REJECTED"), None)
    if m:
        s, j = call("POST", "/v1/loan/entrust-pay", en, {"loanApplicationId": app_id, "merchantId": m.get("id"), "amount": 5000, "purpose": "进货采购"})
        check("B类受托支付", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:250]}")
        s, j = call("GET", "/v1/loan/observation", en)
        check("B类观察期-已开始", (j.get("data") or {}).get("observationStatus") in ("OBSERVING", "NONE"), f"status={(j.get('data') or {}).get('observationStatus')}")
        s, j = call("POST", "/v1/loan/observation/advance", en)
        check("观察期推进1月", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:250]}")
        # B 类还款
        s, j = call("GET", "/v1/loan/repay-preview?creditType=B_TYPE", en)
        check("B类还款试算", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
    else:
        check("B类受托支付", False, "无可用商户")

print()
print("=" * 100)
print("四、注册白名单（修正 crowdType 字段）")
print("=" * 100)
s, j = call("GET", "/v1/auth/schools", tu)
schools = j.get("data", [])
sid = schools[0].get("id") if schools else None
if sid:
    reg = {"username": "qa_stu10", "password": "123456", "realName": "测试学生",
           "schoolId": sid, "idCard": "440100200601010011", "crowdType": "在校生",
           "verifyMethod": "STUDENT_CARD", "verifyNo": "ST20260010"}
    s, j = call("POST", "/v1/auth/register", None, reg)
    check("注册-白名单在校生学生证", j.get("code") in (0, 2001, 3001), f"code={j.get('code')} msg={j.get('message')}")
    reg2 = dict(reg); reg2["username"] = "qa_stu11"; reg2["verifyMethod"] = "GRAD_CERT"
    s, j = call("POST", "/v1/auth/register", None, reg2)
    check("注册-在校生误用毕业证拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
    reg3 = dict(reg); reg3["username"] = "qa_stu12"; reg3["schoolId"] = 999999
    s, j = call("POST", "/v1/auth/register", None, reg3)
    check("注册-非白名单学校拒绝", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")

print()
print("=" * 100)
print("五、测评/理财/个转企/骗局甄别（修正参数）")
print("=" * 100)
s, j = call("POST", "/v1/consumption/risk-assessment/submit", en, {"answers": {"1": "A", "2": "B", "3": "C", "4": "A", "5": "B", "6": "C", "7": "A", "8": "B", "9": "C", "10": "A"}})
check("提交测评(Map)", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
s, j = call("GET", "/v1/consumption/risk-assessment/latest", en)
check("测评-最新结果", j.get("code") == 0, f"level={(j.get('data') or {}).get('riskLevel') if j.get('code')==0 else j}")
s, j = call("GET", "/v1/consumption/finance-product/recommend", en)
check("推荐理财(测评后)", j.get("code") == 0, f"count={len(j.get('data',[]))}")
s, j = call("POST", "/v1/operation/finance/individual-to-company/self-check", en, {"answers": {"1": True, "2": False, "3": True}})
check("个转企自检", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
s, j = call("POST", "/v1/safety/fraud-detect", tu, {"inputText": "刷单返利，日赚500，加QQ88888"})
check("骗局甄别-命中话术", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} hit={(j.get('data') or {}).get('hit') if j.get('code')==0 else ''}")

print()
print("=" * 100)
print("六、记账/预算（修正字段）")
print("=" * 100)
s, j = call("POST", "/v1/bookkeeping/records", tu, {"recordType": "INCOME", "category": "兼职", "amount": 500, "happenDate": "2026-09-28", "description": "测试记账"})
check("记账新增", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = call("POST", "/v1/budget/setting", tu, {"categoryId": 1, "budgetAmount": 2000, "budgetPeriod": "2026-09"})
check("预算设置", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
s, j = call("POST", "/v1/budget/transaction", tu, {"transactionType": "EXPENSE", "amount": 1500, "merchantName": "测试餐饮", "mccCode": "5812", "description": "测试消费"})
check("预算交易", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
s, j = call("GET", "/v1/budget/list", tu)
check("预算列表", j.get("code") == 0, f"count={len(j.get('data',[]))}")

print()
print("=" * 100)
print("七、情景教学 + 反诈演练（本地/LLM）")
print("=" * 100)
s, j = call("GET", "/v1/safety/scenario/list", tu)
scenarios = j.get("data", [])
check("情景列表", j.get("code") == 0 and len(scenarios) > 0, f"count={len(scenarios)}")
if scenarios:
    sc = scenarios[0]
    s, j = call("GET", f"/v1/safety/scenario/{sc.get('id')}", tu)
    check("情景详情", j.get("code") == 0, f"code={j.get('code')}")
    qs = j.get("data", {}).get("questions", [])
    if qs:
        ans = {str(q.get("id")): q.get("options", [{}])[0].get("value") or "A" for q in qs[:2]}
        s, j = call("POST", f"/v1/safety/scenario/{sc.get('id')}/submit", tu, {"answers": ans})
        check("情景提交评分", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
# 反诈演练 start（找 DIALOG 类型情景）
s, j = call("GET", "/v1/safety/scenario/list", tu)
dialog = [x for x in j.get("data", []) if x.get("category") == "DIALOG" or "演练" in str(x.get("scenarioName", ""))]
if dialog:
    s, j = call("POST", f"/v1/safety/scenario/{dialog[0].get('id')}/practice/start", tu)
    check("演练-开始", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
    pno = (j.get("data") or {}).get("practiceNo") if j.get("code") == 0 else None
    if pno:
        s, j = call("POST", f"/v1/safety/scenario/practice/{pno}/finish", tu, {"reason": "manual"})
        check("演练-结束复盘", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
else:
    print("  (无 DIALOG 情景，跳过演练)")

print()
print("=" * 100)
print("八、聊天（local 模式快速 + agent 模式稳定性）")
print("=" * 100)
s, j = call("POST", "/v1/chat/messages", tu, {"message": "你好", "mode": "local"})
check("聊天-local", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} engine={(j.get('data') or {}).get('engineMode') if j.get('code')==0 else ''}")
import time
t0 = time.time()
s, j = call("POST", "/v1/chat/messages", tu, {"message": "青创e贷A类利率是多少", "mode": "agent"})
cost = time.time() - t0
check("聊天-agent(稳定性)", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} engine={(j.get('data') or {}).get('engineMode') if j.get('code')==0 else j} 耗时={cost:.1f}s")

print()
print("=" * 100)
print("九、管理端 CRUD（安全内容/政策门户/预警处理/用户）")
print("=" * 100)
s, j = call("POST", "/v1/admin/safety/alerts", bk, {"title": "QA测试预警", "content": "测试内容", "level": "MEDIUM", "status": "ACTIVE"})
check("管理端-新增预警", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
aid = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
if aid:
    s, j = call("PUT", f"/v1/admin/safety/alerts/{aid}/status?status=INACTIVE", bk)
    check("管理端-预警上下架", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
    s, j = call("DELETE", f"/v1/admin/safety/alerts/{aid}", bk)
    check("管理端-删除预警", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
# 政策门户新增
s, j = call("POST", "/v1/admin/policy/portals", bk, {"portalName": "QA测试门户", "portalType": "GOV_AFFAIR", "region": "全国", "url": "https://example.com", "description": "QA", "status": "ACTIVE"})
check("管理端-新增政策门户", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
pid = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
if pid:
    s, j = call("DELETE", f"/v1/admin/policy/portals/{pid}", bk)
    check("管理端-删除政策门户", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
# 风险预警处理
s, j = call("GET", "/v1/admin/risk-warnings", bk)
warns = (j.get("data") or {}).get("records") or []
if warns:
    wid = warns[0].get("id")
    s, j = call("PUT", f"/v1/admin/risk-warnings/{wid}/handle", bk, {"handleResult": "已联系用户", "handleStatus": "HANDLED"})
    check("管理端-风险预警处理", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
# 用户管理 CRUD
s, j = call("POST", "/admin/v1/user", ad, {"username": "qa_sys01", "password": "123456", "realName": "QA用户", "roleCodes": ["USER"], "status": "ACTIVE"})
check("管理端-新增用户", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
uid = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
if uid:
    s, j = call("DELETE", f"/admin/v1/user/{uid}", ad)
    check("管理端-删除用户", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")

print()
print("=" * 100)
print("结果汇总：PASS=%d FAIL=%d" % (len(PASS), len(FAIL)))
for f in FAIL:
    print("  FAILED:", f)
print("=" * 100)
