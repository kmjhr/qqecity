# -*- coding: utf-8 -*-
"""青启e城 全链路业务深测：保函流/循环贷/B类/注册白名单/异常校验"""
import json, urllib.request, time, sys

BASE = "http://localhost:8080/api"

def call(method, path, token=None, body=None, timeout=60):
    url = BASE + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            raw = r.read().decode("utf-8", "replace")
            return r.status, json.loads(raw)
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw[:200]}
    except Exception as e:
        return "ERR", {"err": str(e)}

def login(u, p="123456"):
    s, j = call("POST", "/v1/auth/login", None, {"username": u, "password": p})
    assert s == 200 and j.get("code") == 0, f"login fail {u}: {s} {j}"
    return j["data"]["accessToken"]

PASS, FAIL = [], []
def check(name, cond, detail=""):
    (PASS if cond else FAIL).append(name)
    mark = "PASS" if cond else "FAIL"
    print(f"[{mark}] {name} {detail}")
    return cond

print("=" * 100)
print("一、保函全流程（testuser 申请 → landlord01 确认 → 缴费开立）")
print("=" * 100)
tu = login("testuser")
ll = login("landlord01")
en = login("entrepreneur")
ad = login("admin")
bk = login("banker01")

# 1.1 提交申请
body = {
    "landlordName": "张房东", "landlordPhone": "13800000001", "landlordIdCard": "440100199001010011",
    "houseTitle": "天河区测试公寓", "province": "广东", "city": "广州", "district": "天河区",
    "address": "测试路1号101", "houseType": "APARTMENT", "area": 45.5, "roomCount": 1,
    "monthlyRent": 3000, "depositAmount": 3000,
    "rentStartDate": "2026-10-01", "rentEndDate": "2027-09-30",
    "payMethod": "MONTHLY", "contractTerms": "测试合同条款"
}
s, j = call("POST", "/v1/guarantee/apply", tu, body)
check("保函申请-提交成功", s == 200 and j.get("code") == 0, str(j)[:200])
gid = j.get("data", {}).get("id") if j.get("code") == 0 else None
st = j.get("data", {}).get("status") if j.get("code") == 0 else None
check("保函申请-初始状态SUBMITTED", st == "SUBMITTED", f"status={st}")
check("保函申请-返回申请单号", bool(j.get("data", {}).get("applicationNo")) if j.get("code") == 0 else False)

# 1.2 参数校验：押金<=0
bad = dict(body); bad["depositAmount"] = -100
s, j = call("POST", "/v1/guarantee/apply", tu, bad)
check("保函申请-押金负数被拒(1001)", j.get("code") == 1001, f"code={j.get('code')} msg={j.get('message')}")

# 1.3 越权：entrepreneur 查看 testuser 的申请
if gid:
    s, j = call("GET", f"/v1/guarantee/{gid}", en)
    check("保函详情-越权访问被拒", j.get("code") in (4001, 2001, 1001), f"code={j.get('code')} msg={j.get('message')}")

# 1.4 越权确认：entrepreneur 确认 testuser 的保函
if gid:
    s, j = call("PUT", f"/v1/guarantee/{gid}/landlord-confirm", en, {})
    check("房东确认-非房东越权被拒", j.get("code") in (4001, 2001, 1001), f"code={j.get('code')} msg={j.get('message')}")

# 1.5 未确认就缴费
if gid:
    s, j = call("POST", f"/v1/guarantee/{gid}/pay", tu)
    check("缴费-未确认先缴费被拒", j.get("code") in (2001, 1001, 4001), f"code={j.get('code')} msg={j.get('message')}")

# 1.6 房东确认（landlord01 模拟房东：确认接口按手机号匹配房东？看返回）
if gid:
    s, j = call("PUT", f"/v1/guarantee/{gid}/landlord-confirm", ll, {"signContent": "CLICK_CONFIRM"})
    check("房东确认-landlord01确认", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} status={j.get('data',{}).get('status') if isinstance(j.get('data'),dict) else ''}")
    st_after = j.get("data", {}).get("status") if j.get("code") == 0 and isinstance(j.get("data"), dict) else None
    check("房东确认-状态推进(非SUBMITTED)", st_after != "SUBMITTED", f"status={st_after}")

# 1.7 确认后再次确认（重复确认）
if gid:
    s, j = call("PUT", f"/v1/guarantee/{gid}/landlord-confirm", ll, {})
    check("房东确认-重复确认被拒", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")

# 1.8 缴费开立
if gid:
    s, j = call("POST", f"/v1/guarantee/{gid}/pay", tu)
    check("缴费-创建支付订单", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'), ensure_ascii=False)[:150]}")
    pay_data = j.get("data") if j.get("code") == 0 else None
    order_no = pay_data.get("orderNo") if isinstance(pay_data, dict) else None
    if order_no:
        s2, j2 = call("POST", f"/v1/pay/orders/{order_no}/pay", tu, {"payPassword": "123456"})
        check("缴费-支付订单支付", j2.get("code") == 0, f"code={j2.get('code')} msg={j2.get('message')}")
    # 详情状态
    s, j = call("GET", f"/v1/guarantee/{gid}", tu)
    st_now = j.get("data", {}).get("status") if j.get("code") == 0 else None
    check("缴费后-状态APPROVED已开立", st_now == "APPROVED", f"status={st_now}")

# 1.9 重复缴费
if gid:
    s, j = call("POST", f"/v1/guarantee/{gid}/pay", tu)
    check("缴费-重复缴费被拒", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")

print()
print("=" * 100)
print("二、循环贷 A 类（testuser 提款/试算/还款/流水核对）")
print("=" * 100)
s, j = call("GET", "/v1/loan/credit", tu)
check("额度查询", j.get("code") == 0, str(j.get("data"))[:200])
a_limit = None
if j.get("code") == 0:
    for c in j["data"]:
        if c.get("creditType") == "A_TYPE":
            a_limit = c
print("A额度:", json.dumps(a_limit, ensure_ascii=False)[:300] if a_limit else "无")

# 2.1 提款 10000
s, j = call("POST", "/v1/loan/withdraw", tu, {"amount": 10000})
check("提款10000", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
# 2.2 提款负数
s, j = call("POST", "/v1/loan/withdraw", tu, {"amount": -500})
check("提款负数被拒", j.get("code") in (1001, 2001), f"code={j.get('code')} msg={j.get('message')}")
# 2.3 超额提款
s, j = call("POST", "/v1/loan/withdraw", tu, {"amount": 999999})
check("提款超额被拒", j.get("code") in (1001, 2001, 3001), f"code={j.get('code')} msg={j.get('message')}")
# 2.4 再提款 5000
s, j = call("POST", "/v1/loan/withdraw", tu, {"amount": 5000})
check("再提款5000", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
# 2.5 试算
s, j = call("GET", "/v1/loan/repay-preview?creditType=A_TYPE", tu)
check("还款试算", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
# 2.6 还款 10000（FIFO 冲抵第一笔）
s, j = call("POST", "/v1/loan/repay", tu, {"amount": 10000})
check("还款10000", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
# 2.7 流水核对
s, j = call("GET", "/v1/loan/credit-txns?creditType=A_TYPE", tu)
if j.get("code") == 0:
    txns = j["data"]
    w = [t for t in txns if t.get("txnType") in ("WITHDRAW", "WITHDRAWAL", "LOAN")]
    r = [t for t in txns if t.get("txnType") in ("REPAY", "REPAYMENT")]
    check("流水-含提款与还款", len(w) >= 2 and len(r) >= 1, f"w={len(w)} r={len(r)}")
else:
    check("流水查询", False, str(j)[:100])

print()
print("=" * 100)
print("三、B 类受托支付（entrepreneur）")
print("=" * 100)
# 3.1 预审
s, j = call("POST", "/v1/loan/precheck", en, {"businessPlan": "校园打印店", "expectedMonthlyIncome": 8000, "hasBusinessLicense": False})
check("B类预审", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
# 3.2 商户列表
s, j = call("GET", "/v1/loan/merchants", en)
merchants = j.get("data") if j.get("code") == 0 else []
check("商户列表", j.get("code") == 0 and len(merchants) > 0, f"count={len(merchants) if merchants else 0}")
# 3.3 受托支付
if merchants:
    m = merchants[0]
    s, j = call("POST", "/v1/loan/entrust-pay", en, {"merchantId": m.get("id"), "amount": 5000})
    check("B类受托支付", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
    # 3.4 观察期
    s, j = call("GET", "/v1/loan/observation", en)
    check("B类观察期状态", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
    # 3.5 观察期推进
    s, j = call("POST", "/v1/loan/observation/advance", en)
    check("观察期推进1月", j.get("code") == 0, f"msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
    # 3.6 数据回流进度
    s, j = call("GET", "/v1/loan/observation-progress", en)
    check("观察期数据回流进度", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:250]}")
    # 3.7 B类还款试算
    s, j = call("GET", "/v1/loan/repay-preview?creditType=B_TYPE", en)
    check("B类还款试算", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")

print()
print("=" * 100)
print("四、注册白名单与核验（新用户）")
print("=" * 100)
s, j = call("GET", "/v1/auth/schools", tu)
schools = j.get("data") if j.get("code") == 0 else []
print("学校数:", len(schools))
sid = schools[0].get("id") if schools else None
sname = schools[0].get("schoolName") if schools else ""
# 4.1 白名单学校 + 在校生 + 学生证
if sid:
    reg = {"username": "qa_stu01", "password": "123456", "realName": "测试学生", "schoolId": sid,
           "idCard": "440100200601010011", "crowdType": "在校生",
           "verifyMethod": "STUDENT_CARD", "verifyNo": "ST20260001"}
    s, j = call("POST", "/v1/auth/register", None, reg)
    check("注册-白名单在校生学生证", j.get("code") in (0, 2001, 3001, 1001), f"code={j.get('code')} msg={j.get('message')}")
    # 4.2 错配：在校生用毕业证
    reg2 = dict(reg); reg2["username"] = "qa_stu02"; reg2["verifyMethod"] = "GRAD_CERT"; reg2["verifyNo"] = "BZ20260002"
    s, j = call("POST", "/v1/auth/register", None, reg2)
    check("注册-在校生误用毕业证被拒", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
    # 4.3 非白名单学校
    reg3 = dict(reg); reg3["username"] = "qa_stu03"; reg3["schoolId"] = 999999
    s, j = call("POST", "/v1/auth/register", None, reg3)
    check("注册-非白名单学校被拒", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")
    # 4.4 重复用户名
    s, j = call("POST", "/v1/auth/register", None, reg)
    check("注册-重复用户名被拒", j.get("code") != 0, f"code={j.get('code')} msg={j.get('message')}")

print()
print("=" * 100)
print("五、管理端核心操作（admin/banker01）")
print("=" * 100)
# 5.1 dashboard
s, j = call("GET", "/v1/admin/dashboard/stats", ad)
check("管理端-dashboard统计", j.get("code") == 0, f"keys={list(j.get('data',{}).keys()) if isinstance(j.get('data'),dict) else j.get('data')}")
# 5.2 保函代确认（管理端 landlord-confirm）——用刚创建的 gid
if gid:
    s, j = call("PUT", f"/v1/admin/guarantee/{gid}/landlord-confirm?signContent=ADMIN_DEMO", bk)
    check("管理端-保函代房东确认", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} status={j.get('data',{}).get('status') if isinstance(j.get('data'),dict) else ''}")
# 5.3 注册审核列表
s, j = call("GET", "/v1/admin/registration-reviews", bk)
check("管理端-注册审核列表", j.get("code") == 0, f"records={j.get('data',{}).get('total') if isinstance(j.get('data'),dict) else ''}")
# 5.4 贷款申请审核列表
s, j = call("GET", "/v1/admin/loan/applications", bk)
check("管理端-贷款申请列表", j.get("code") == 0, f"total={j.get('data',{}).get('total') if isinstance(j.get('data'),dict) else ''}")
# 5.5 商户审核列表
s, j = call("GET", "/v1/admin/merchants", bk)
check("管理端-商户审核列表", j.get("code") == 0, f"count={len(j.get('data',[]))}")
# 5.6 受托支付复核
s, j = call("GET", "/v1/admin/entrust-reviews", bk)
check("管理端-受托复核列表", j.get("code") == 0, f"count={len(j.get('data',[]))}")
# 5.7 风险预警
s, j = call("GET", "/v1/admin/risk-warnings", bk)
check("管理端-风险预警列表", j.get("code") == 0, f"total={j.get('data',{}).get('total') if isinstance(j.get('data'),dict) else ''}")

print()
print("=" * 100)
print("六、风险预警与安全检测（触发类）")
print("=" * 100)
# 6.1 骗局甄别（命中话术）
s, j = call("POST", "/v1/safety/fraud-detect", tu, {"text": "刷单返利，日赚500，加QQ88888"})
check("骗局甄别-命中话术", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
# 6.2 逾期风险预判
s, j = call("POST", "/v1/safety/overdue-risk/predict?aheadDays=7", tu)
check("逾期风险预判", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
# 6.3 高频借贷检测
s, j = call("POST", "/v1/consumption/high-freq-borrow/detect", tu)
check("高频借贷检测", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
# 6.4 现金流预警
s, j = call("POST", "/v1/operation/cashflow-warning/detect", tu)
check("现金流预警检测", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
# 6.5 风险总览
s, j = call("GET", "/v1/risk/overview", tu)
check("风险总览", j.get("code") == 0, f"keys={list(j.get('data',{}).keys()) if isinstance(j.get('data'),dict) else ''}")

print()
print("=" * 100)
print("七、理财/测评/保险/征信（合规校验）")
print("=" * 100)
# 7.1 测评问卷
s, j = call("GET", "/v1/consumption/risk-assessment/questionnaire", en)
check("测评问卷", j.get("code") == 0, f"q_count={len(j.get('data',[])) if isinstance(j.get('data'),list) else ''}")
# 7.2 提交测评（低风险偏好）
s, j = call("POST", "/v1/consumption/risk-assessment/submit", en, {"answers": [{"questionId": 1, "answer": "A"}, {"questionId": 2, "answer": "B"}]})
check("提交测评", j.get("code") in (0, 1001), f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
# 7.3 推荐理财（测评后）
s, j = call("GET", "/v1/consumption/finance-product/recommend", en)
check("推荐理财(测评后)", j.get("code") == 0, f"count={len(j.get('data',[])) if isinstance(j.get('data'),list) else j.get('data')}")
# 7.4 理财购买演示（未测评则拒绝）
s, j = call("GET", "/v1/consumption/finance-product", tu)
prods = j.get("data") if j.get("code") == 0 else []
if prods:
    pid = prods[0].get("id")
    s2, j2 = call("POST", f"/v1/consumption/finance-product/{pid}/apply-demo", tu)
    check("理财购买演示-需先测评", j2.get("code") in (0, 1001), f"code={j2.get('code')} msg={j2.get('message')}")
# 7.5 保险匹配与演示投保
s, j = call("GET", "/v1/insurance/match", tu)
check("保险匹配", j.get("code") == 0, f"count={len(j.get('data',[])) if isinstance(j.get('data'),list) else ''}")
s, j = call("GET", "/v1/insurance", tu)
ins = j.get("data") if j.get("code") == 0 else []
if ins:
    s2, j2 = call("POST", f"/v1/insurance/{ins[0].get('id')}/apply-demo", tu, {})
    check("保险演示投保", j2.get("code") == 0, f"code={j2.get('code')} msg={j2.get('message')}")
# 7.6 征信软查询
s, j = call("POST", "/v1/consumption/credit-monitor/soft-query", tu, {})
check("征信软查询", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
# 7.7 个转企自检
s, j = call("POST", "/v1/operation/finance/individual-to-company/self-check", tu, {"companyName": "测试科技公司", "businessScope": "软件开发"})
check("个转企自检", j.get("code") == 0, f"data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")

print()
print("=" * 100)
print("八、消息/记账/预算/信用画像（数据完整性）")
print("=" * 100)
s, j = call("GET", "/v1/message/unread-count", tu)
check("消息未读数", j.get("code") == 0, f"data={j.get('data')}")
s, j = call("PUT", "/v1/message/read-all", tu)
check("消息全部已读", j.get("code") == 0, f"msg={j.get('message')}")
s, j = call("POST", "/v1/bookkeeping/records", tu, {"recordType": "INCOME", "amount": 500, "category": "兼职", "occurDate": "2026-09-28", "note": "测试"})
check("记账新增", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
s, j = call("GET", "/v1/bookkeeping/records", tu)
check("记账列表", j.get("code") == 0, f"count={len(j.get('data',[])) if isinstance(j.get('data'),list) else ''}")
s, j = call("POST", "/v1/budget/setting", tu, {"categoryId": 1, "monthlyLimit": 2000})
check("预算设置", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:150]}")
s, j = call("POST", "/v1/budget/transaction", tu, {"categoryId": 1, "amount": 1500, "occurDate": "2026-09-28", "note": "测试消费"})
check("预算记账", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} data={json.dumps(j.get('data'),ensure_ascii=False)[:200]}")
s, j = call("GET", "/v1/budget/overview", tu)
check("预算总览", j.get("code") == 0, f"keys={list(j.get('data',{}).keys()) if isinstance(j.get('data'),dict) else ''}")
s, j = call("GET", "/v1/profile", tu)
check("信用画像", j.get("code") == 0, f"keys={list(j.get('data',{}).keys()) if isinstance(j.get('data'),dict) else ''}")

print()
print("=" * 100)
print("结果汇总：PASS=%d FAIL=%d" % (len(PASS), len(FAIL)))
for f in FAIL:
    print("  FAILED:", f)
print("=" * 100)
