# -*- coding: utf-8 -*-
"""索赔全流程最终复测（guaranteeId=保函id, decision字段）+ 越权验证"""
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

tu = login("testuser"); ll = login("landlord01"); bk = login("banker01"); en = login("entrepreneur")

print("=" * 100)
print("一、越权修复验证")
print("=" * 100)
s, j = call("GET", "/v1/guarantee/claims/manual-review-queue", tu)
check("普通用户访问复核队列-拒绝(4001)", j.get("code") == 4001, f"code={j.get('code')} msg={j.get('message')}")
s, j = call("GET", "/v1/guarantee/claims/manual-review-queue", en)
check("创业者访问复核队列-拒绝(4001)", j.get("code") == 4001, f"code={j.get('code')} msg={j.get('message')}")
s, j = call("GET", "/v1/guarantee/claims/manual-review-queue", bk)
check("banker访问复核队列-正常", j.get("code") == 0, f"code={j.get('code')}")
# 普通用户直接调复核接口
s, j = call("GET", "/v1/guarantee/claims?status=MANUAL_REVIEW", tu)
recs = (j.get("data") or {}).get("records", [])
if recs:
    cid = recs[0].get("id")
    s, j = call("PUT", f"/v1/guarantee/claims/{cid}/review", tu, {"decision": "APPROVED", "payoutAmount": 100})
    check("普通用户人工复核-拒绝(4001)", j.get("code") == 4001, f"code={j.get('code')} msg={j.get('message')}")

print()
print("=" * 100)
print("二、索赔全流程（正确参数）")
print("=" * 100)
# 找 testuser 的 APPROVED 保函（详情取 guaranteeId）
s, j = call("GET", "/v1/guarantee/page", tu)
apps = [x for x in (j.get("data") or {}).get("records", []) if x.get("applyStatus") == "APPROVED"]
gid = apps[0].get("id")
s, j = call("GET", f"/v1/guarantee/{gid}", tu)
guarantee_id = (j.get("data") or {}).get("guaranteeId")
print("保函guaranteeId:", guarantee_id)
body = {"guaranteeId": guarantee_id, "claimAmount": 1500, "claimReason": "租客提前退租且未结清水电费",
        "evidenceFiles": "[{\"name\":\"水电费清单.jpg\"},{\"name\":\"沟通记录.png\"}]"}
s, j = call("POST", "/v1/guarantee/claims", ll, body)
check("房东发起索赔", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
claim_id = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
st = (j.get("data") or {}).get("claimStatus") if j.get("code") == 0 else None
check("AI初审后-DEFENSE_PERIOD(存疑)", st == "DEFENSE_PERIOD", f"claimStatus={st}")
if claim_id:
    # 租客申辩
    s, j = call("PUT", f"/v1/guarantee/claims/{claim_id}/defense", tu, {"defenseContent": "水电费已结清，有转账记录"})
    check("租客申辩", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} status={(j.get('data') or {}).get('claimStatus') if j.get('code')==0 else ''}")
    st2 = (j.get("data") or {}).get("claimStatus") if j.get("code") == 0 else None
    check("申辩后→MANUAL_REVIEW", st2 == "MANUAL_REVIEW", f"claimStatus={st2}")
    # banker 复核队列能看到
    s, j = call("GET", "/v1/guarantee/claims/manual-review-queue", bk)
    queued = any(x.get("id") == claim_id for x in (j.get("data") or {}).get("records", []))
    check("复核队列可见该索赔", queued, f"code={j.get('code')}")
    # banker 人工复核赔付
    s, j = call("PUT", f"/v1/guarantee/claims/{claim_id}/review", bk, {"decision": "APPROVED", "payoutAmount": 1200})
    check("banker复核-赔付", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} status={(j.get('data') or {}).get('claimStatus') if j.get('code')==0 else ''}")
    st3 = (j.get("data") or {}).get("claimStatus") if j.get("code") == 0 else None
    check("复核后→CLOSED已结案", st3 == "CLOSED", f"claimStatus={st3}")
    # 详情确认赔付金额
    s, j = call("GET", f"/v1/guarantee/claims/{claim_id}", tu)
    payout = (j.get("data") or {}).get("payoutAmount") if j.get("code") == 0 else None
    check("赔付金额1200正确", payout == 1200.0, f"payout={payout}")
    # 租客消息通知
    s, j = call("GET", "/v1/message/page", tu)
    msgs = (j.get("data") or {}).get("records", [])
    has_claim = any("索赔" in str(x.get("title", "")) for x in msgs)
    check("租客收到索赔通知", has_claim, f"msgs={len(msgs)}")

print()
print("=" * 100)
print("结果：PASS=%d FAIL=%d" % (len(PASS), len(FAIL)))
for f in FAIL: print("  FAILED:", f)
print("=" * 100)
