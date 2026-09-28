# -*- coding: utf-8 -*-
"""索赔全流程（7态）+ 管理端复核队列越权检查"""
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
print("一、索赔 7 态流转")
print("=" * 100)
s, j = call("GET", f"/v1/guarantee/claims/status-flow", tu)
print("状态流:", json.dumps(j.get("data", {}).get("flow") if j.get("code")==0 else j, ensure_ascii=False)[:300])
# 用之前的 APPROVED 保函（id=12）
s, j = call("GET", "/v1/guarantee/page?status=APPROVED", tu)
apps = [x for x in (j.get("data") or {}).get("records", []) if x.get("applyStatus") == "APPROVED"]
print("已开立保函数:", len(apps))
if apps:
    gid = apps[0].get("id")
    body = {"guaranteeId": gid, "claimAmount": 1500, "claimReason": "租客提前退租且未结清水电费", "evidenceFiles": "[{\"name\":\"水电费清单.jpg\",\"url\":\"demo://1\"}]"}
    s, j = call("POST", "/v1/guarantee/claims", ll, body)
    check("房东发起索赔", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')}")
    claim_id = (j.get("data") or {}).get("id") if j.get("code") == 0 else None
    st = (j.get("data") or {}).get("claimStatus") if j.get("code") == 0 else None
    check("AI初审后状态(SUBMITTED/AI_REVIEW/DEFENSE_PERIOD)", st in ("SUBMITTED", "AI_REVIEW", "DEFENSE_PERIOD", "APPROVED"), f"claimStatus={st}")
    # 租客申辩
    s, j = call("PUT", f"/v1/guarantee/claims/{claim_id}/defense", tu, {"defenseContent": "水电费已结清，有转账记录"})
    check("租客提交申辩", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} status={(j.get('data') or {}).get('claimStatus') if j.get('code')==0 else ''}")
    st2 = (j.get("data") or {}).get("claimStatus") if j.get("code") == 0 else None
    check("申辩后→MANUAL_REVIEW", st2 == "MANUAL_REVIEW", f"claimStatus={st2}")
    # banker 人工复核：赔付
    s, j = call("PUT", f"/v1/guarantee/claims/{claim_id}/review", bk, {"reviewDecision": "APPROVED", "payoutAmount": 1200, "reviewNote": "部分赔付"})
    check("banker复核-赔付", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} status={(j.get('data') or {}).get('claimStatus') if j.get('code')==0 else ''}")
    st3 = (j.get("data") or {}).get("claimStatus") if j.get("code") == 0 else None
    check("赔付后状态APPROVED", st3 == "APPROVED", f"claimStatus={st3}")
    # 查询列表
    s, j = call("GET", "/v1/guarantee/claims", ll)
    check("房东索赔列表可见", j.get("code") == 0 and any((x.get("id")) == claim_id for x in (j.get("data") or {}).get("records", [])), f"code={j.get('code')}")
    # 结案
    s, j = call("PUT", f"/v1/guarantee/claims/{claim_id}/close", bk)
    check("结案", j.get("code") == 0, f"code={j.get('code')} msg={j.get('message')} status={(j.get('data') or {}).get('claimStatus') if j.get('code')==0 else ''}")

print()
print("=" * 100)
print("二、复核队列越权检查")
print("=" * 100)
s, j = call("GET", "/v1/guarantee/claims/manual-review-queue", tu)
check("普通用户访问复核队列-拒绝", j.get("code") in (4001, 1002), f"code={j.get('code')} msg={j.get('message')}")
s, j = call("GET", "/v1/guarantee/claims/manual-review-queue", bk)
check("banker访问复核队列-正常", j.get("code") == 0, f"code={j.get('code')} total={(j.get('data') or {}).get('total') if j.get('code')==0 else ''}")

print()
print("=" * 100)
print("结果：PASS=%d FAIL=%d" % (len(PASS), len(FAIL)))
for f in FAIL: print("  FAILED:", f)
print("=" * 100)
