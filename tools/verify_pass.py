#!/usr/bin/env python3
import bcrypt

# 从 SQL 文件中提取的哈希
hashed = 'REMOVED_BCRYPT_HASH'

# 测试密码
test_passwords = [
    'REMOVED',
    'REMOVED',
    'admin',
    '123456',
]

print("测试密码匹配：")
for pwd in test_passwords:
    result = bcrypt.checkpw(pwd.encode('utf-8'), hashed.encode('utf-8'))
    status = "✓ 匹配" if result else "✗ 不匹配"
    print(f"  {pwd}: {status}")
