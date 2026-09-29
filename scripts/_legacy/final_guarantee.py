# -*- coding: utf-8 -*-
"""保函缴费闭环最终验证（password 字段）+ 电子保函详情"""
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

tu = login("testuser"); ll = login("landlord01")
body = {"landlordName": "赵房东", "landlordPhone": "13900000003", "landlordIdCard": "440100197301010011",
        "houseTitle": "荔湾区测试公寓", "province": "广东", "city": "广州", "district": "荔湾区",
        "address": "测试路6号606", "houseType": "APARTMENT", "area": 45, "roomCount": 1,
        "monthlyRent": 3000, "depositAmount": 3000, "rentStartDate": "2027-02-01", "rentEndDate": "2028-01-31",
        "payMethod": "MONTHLY", "contractTerms": "测试条款"}
s, j = call("POST", "/v1/guarantee/apply", tu, body)
gid = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
print("申请:", j.get("code"), j.get("message"))
s, j = call("PUT", f"/v1/guarantee/{gid}/landlord-confirm", ll, {"signContent": "CLICK_CONFIRM"})
print("确认:", j.get("code"), j.get("message"), (j.get("data") or {}).get("applyStatus"))
s, j = call("POST", f"/v1/guarantee/{gid}/pay", tu)
order_no = (j.get("data") or {}).get("orderNo") if j.get("code") == 0 else None
print("订单:", j.get("code"), j.get("message"), order_no)
s, j = call("POST", f"/v1/pay/orders/{order_no}/pay", tu, {"payMethod": "CARD", "password": "123456"})
print("支付:", j.get("code"), j.get("message"), json.dumps(j.get("data"), ensure_ascii=False)[:150])
s, j = call("GET", f"/v1/guarantee/{gid}", tu)
st = (j.get("data") or {}).get("applyStatus")
print("保函状态:", st)
gno = (j.get("data") or {}).get("guaranteeId")
s, j = call("GET", f"/v1/guarantee/guarantee/{gno}", tu)
print("电子保函:", j.get("code"), json.dumps(j.get("data"), ensure_ascii=False)[:200])
# 状态流转
s, j = call("GET", "/v1/guarantee/status-flow", tu)
print("状态流:", j.get("code"), json.dumps(j.get("data"), ensure_ascii=False)[:150])
print("最终:", "PASS" if st == "APPROVED" else "FAIL")
