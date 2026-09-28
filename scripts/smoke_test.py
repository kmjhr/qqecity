# -*- coding: utf-8 -*-
"""青启e城 全链路冒烟测试：多账号遍历 GET 接口，记录 HTTP/code/data 摘要"""
import json, urllib.request, urllib.error, ssl, sys, time

BASE = "http://localhost:8080/api"
ctx = ssl.create_default_context()

def call(method, path, token=None, body=None, timeout=30):
    url = BASE + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            raw = r.read().decode("utf-8", "replace")
            try:
                j = json.loads(raw)
                code = j.get("code", "?")
                msg = j.get("message", "")
                data = j.get("data")
                summary = ""
                if isinstance(data, dict):
                    keys = ",".join(list(data.keys())[:6])
                    summary = "keys=" + keys
                elif isinstance(data, list):
                    summary = f"list[{len(data)}]"
                elif data is None:
                    summary = "null"
                else:
                    summary = str(data)[:60]
                return r.status, f"code={code} msg={msg} {summary}"
            except Exception:
                return r.status, raw[:80]
    except urllib.error.HTTPError as e:
        try:
            j = json.loads(e.read().decode("utf-8", "replace"))
            return e.code, f"code={j.get('code')} msg={j.get('message')}"
        except Exception:
            return e.code, "HTTPError"
    except Exception as e:
        return "ERR", str(e)[:60]

def login(username, password="123456"):
    code, info = call("POST", "/v1/auth/login", None, {"username": username, "password": password})
    if code != 200:
        return None, f"login fail {code} {info}"
    # 解析 token
    url = BASE + "/v1/auth/login"
    req = urllib.request.Request(url, data=json.dumps({"username": username, "password": password}).encode(), method="POST")
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=15) as r:
        j = json.loads(r.read().decode("utf-8", "replace"))
    return j["data"]["accessToken"], "ok"

# 无参 GET 接口清单（含管理端）
GET_PATHS = [
    # auth/user/message
    "/v1/auth/schools", "/v1/user/profile", "/v1/message/unread-count", "/v1/message/page",
    # guarantee
    "/v1/guarantee/status-flow", "/v1/guarantee/page",
    # loan
    "/v1/loan/product-rules", "/v1/loan/credit", "/v1/loan/merchants", "/v1/loan/entrust-records",
    "/v1/loan/applications", "/v1/loan/credit-txns", "/v1/loan/observation", "/v1/loan/observation-progress",
    # bookkeeping / budget
    "/v1/bookkeeping/records", "/v1/bookkeeping/cashflow-report",
    "/v1/budget/categories", "/v1/budget/savings", "/v1/budget/overview", "/v1/budget/repay-guard",
    # safety
    "/v1/safety/alerts", "/v1/safety/featured-cases", "/v1/safety/anti-fraud/list",
    "/v1/safety/credit-report/reports", "/v1/safety/overdue-risk/calendar", "/v1/safety/overdue-risk/warnings",
    "/v1/safety/credit-health", "/v1/safety/scenario/list", "/v1/safety/scenario/practice/history",
    # policy / insurance / operation
    "/v1/policy/match", "/v1/policy/matched", "/v1/policy/portal", "/v1/policy/portal/regions",
    "/v1/insurance/match", "/v1/insurance/matched", "/v1/insurance",
    "/v1/operation/finance/content", "/v1/operation/finance/individual-to-company/checklist",
    "/v1/operation/loan-linked/warnings", "/v1/operation/cashflow-warning/history",
    # cashflow / consumption
    "/v1/cashflow/aggregate/auth", "/v1/cashflow/aggregate/report", "/v1/cashflow/anti-brush/overview",
    "/v1/consumption/finance-product", "/v1/consumption/finance-product/recommend",
    "/v1/consumption/finance-product/recommend/by-level",
    "/v1/consumption/risk-assessment/questionnaire", "/v1/consumption/risk-assessment/latest",
    "/v1/consumption/risk-assessment/history",
    "/v1/consumption/credit-monitor/latest", "/v1/consumption/credit-monitor/reports",
    "/v1/consumption/credit-monitor/warnings", "/v1/consumption/guide/monthly-bill",
    "/v1/consumption/guide/pay-before-reminder", "/v1/consumption/high-freq-borrow/history",
    # chat / pay / profile / risk
    "/v1/chat/engine-status", "/v1/chat/history",
    "/v1/pay/wallet", "/v1/pay/wallet/transactions", "/v1/pay/orders",
    "/v1/profile", "/v1/risk/overview",
    # admin（admin + banker01）
    "/v1/admin/dashboard/stats", "/v1/admin/guarantee/applications", "/v1/admin/registration-reviews",
    "/v1/admin/loan/applications", "/v1/admin/loan/credit-txns", "/v1/admin/loan/entrust-payments",
    "/v1/admin/merchants", "/v1/admin/entrust-reviews", "/v1/admin/risk-warnings",
    "/v1/admin/guarantee/manual-review-queue", "/v1/admin/safety/alerts", "/v1/admin/safety/contents",
    "/v1/admin/policy/portals", "/admin/v1/user/page",
    "/v1/pay/admin/orders", "/v1/pay/admin/transactions", "/v1/pay/admin/merchant-accounts",
]

def main():
    users = ["testuser", "entrepreneur", "landlord01", "admin", "banker01"]
    tokens = {}
    for u in users:
        t, info = login(u)
        if t:
            tokens[u] = t
            print(f"[login] {u}: ok")
        else:
            print(f"[login] {u}: {info}")
    print("=" * 120)
    print(f"{'接口':<62}{'testuser':<28}{'entrepreneur':<28}{'landlord01':<28}")
    print("-" * 120)
    results = []
    for p in GET_PATHS:
        line = p
        cells = []
        for u in ["testuser", "entrepreneur", "landlord01"]:
            if u not in tokens:
                cells.append("no-token")
                continue
            s, info = call("GET", p, tokens[u])
            cells.append(f"{s} {info[:24]}")
        print(f"{line:<62}{cells[0]:<28}{cells[1]:<28}{cells[2]:<28}")
        results.append((p, cells))
    # admin 接口用 admin/banker01
    print("-" * 120)
    print("=== admin/banker01 管理接口 ===")
    for p in GET_PATHS:
        if "/admin/" in p or p in ("/admin/v1/user/page", "/v1/pay/admin/orders", "/v1/pay/admin/transactions", "/v1/pay/admin/merchant-accounts"):
            cells = []
            for u in ["admin", "banker01"]:
                if u not in tokens:
                    cells.append("no-token")
                    continue
                s, info = call("GET", p, tokens[u])
                cells.append(f"{s} {info[:30]}")
            print(f"{p:<62}{cells[0]:<34}{cells[1]:<34}")
    # 普通用户越权访问管理端
    print("-" * 120)
    print("=== 越权测试：testuser 访问管理端接口（期望 4001） ===")
    for p in ["/v1/admin/dashboard/stats", "/v1/admin/merchants", "/admin/v1/user/page", "/v1/pay/admin/orders"]:
        if "testuser" in tokens:
            s, info = call("GET", p, tokens["testuser"])
            print(f"{p:<62}{s} {info}")

if __name__ == "__main__":
    main()
