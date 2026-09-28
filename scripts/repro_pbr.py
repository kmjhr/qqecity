# -*- coding: utf-8 -*-
"""复现 pay-before-reminder 5000 + 记录时间戳"""
import json, urllib.request, time

BASE = "http://localhost:8080/api"
def call(method, path, token=None, body=None, timeout=30):
    req = urllib.request.Request(BASE + path, data=json.dumps(body).encode() if body is not None else None, method=method)
    req.add_header("Content-Type", "application/json")
    if token: req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")

s, raw = call("POST", "/v1/auth/login", None, {"username": "testuser", "password": "123456"})
tok = json.loads(raw)["data"]["accessToken"]
print("now=", time.strftime("%H:%M:%S"))
s, raw = call("GET", "/v1/consumption/guide/pay-before-reminder", tok)
print("HTTP", s, raw[:300])
