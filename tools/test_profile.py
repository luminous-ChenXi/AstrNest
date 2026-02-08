#!/usr/bin/env python3
"""测试获取用户资料 API"""

import requests
import base64

# 使用 Basic Auth
username = "admin"
password = "chenxi123"
credentials = base64.b64encode(f"{username}:{password}".encode()).decode()

url = "http://localhost:8080/api/user/profile"
headers = {
    "Authorization": f"Basic {credentials}"
}

print("=" * 60)
print("测试获取用户资料 API")
print("=" * 60)
print(f"\n请求: GET {url}")
print(f"Authorization: Basic {credentials}")

try:
    response = requests.get(url, headers=headers, timeout=10)
    print(f"\n状态码: {response.status_code}")
    if response.status_code == 200:
        print(f"结果: ✓ 成功")
        print(f"响应: {response.text[:500]}")
    else:
        print(f"结果: ✗ 失败")
        print(f"响应: {response.text}")
except Exception as e:
    print(f"错误: {e}")

print("\n" + "=" * 60)
