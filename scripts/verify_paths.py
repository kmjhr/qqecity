# -*- coding: utf-8 -*-
"""验证 policy portal 路径 + 两个5000接口 + 不存在路径对照"""
import json, urllib.request, ssl

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
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:
        return "ERR", str(e)

def login(u, p="123456"):
    s, raw = call("POST", "/v1/auth/login", None, {"username": u, "password": p})
    return json.loads(raw)["data"]["accessToken"]

tok = login("testuser")
paths = [
    "/v1/policy/portals",
    "/v1/policy/portals/regions",
    "/v1/policy/portal",
    "/v1/policy/portal/regions",
    "/v1/nonexistent-path-xyz",
    "/v1/consumption/finance-product/recommend/by-level",
    "/v1/consumption/guide/pay-before-reminder",
]
for p in paths:
    s, raw = call("GET", p, tok)
    print(f"{p:<60} HTTP={s} body={raw[:220]}")
