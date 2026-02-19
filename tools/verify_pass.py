#!/usr/bin/env python3
import bcrypt

# 从 SQL 文件中提取的哈希
hashed = '$2b$12$8WorkA9WmkwFNcJ3WZIGeeG94iXBNLFmi8hss0RXrZGbO9Ni4jc2i'

# 测试密码
test_passwords = [
    'chenxi123',
    'chenxi123',
    'admin',
    '123456',
]

print("测试密码匹配：")
for pwd in test_passwords:
    result = bcrypt.checkpw(pwd.encode('utf-8'), hashed.encode('utf-8'))
    status = "✓ 匹配" if result else "✗ 不匹配"
    print(f"  {pwd}: {status}")
