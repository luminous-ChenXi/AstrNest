#!/usr/bin/env python3
"""直接测试登录 API"""

import requests
import json

# 测试登录
url = "http://localhost:8080/api/auth/login"
headers = {"Content-Type": "application/json"}

# 测试不同的密码组合
test_cases = [
    {"username": "admin", "password": "chenxi123"},
    {"username": "admin", "password": "chenxi123!"},
    {"username": "admin@example.com", "password": "chenxi123"},
]

print("=" * 60)
print("登录 API 测试")
print("=" * 60)

for i, data in enumerate(test_cases, 1):
    print(f"\n测试 {i}: username={data['username']}, password={data['password']}")
    try:
        response = requests.post(url, json=data, headers=headers, timeout=10)
        print(f"  状态码: {response.status_code}")
        if response.status_code == 200:
            print(f"  结果: ✓ 登录成功")
            print(f"  响应: {json.dumps(response.json(), indent=2, ensure_ascii=False)[:200]}...")
        else:
            print(f"  结果: ✗ 登录失败")
            print(f"  响应: {response.text[:200]}")
    except Exception as e:
        print(f"  错误: {e}")

print("\n" + "=" * 60)
