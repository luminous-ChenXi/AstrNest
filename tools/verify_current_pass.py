#!/usr/bin/env python3
"""验证数据库中当前存储的密码哈希"""

import bcrypt

# 数据库中的密码哈希
hashed = '$2b$12$1uLnox51dsclaN4VP7wQnen64wtIuBZyp98vltJgmOgEoTjC6/En2'

# 测试可能的密码
test_passwords = [
    'chenxi123',    # 用户输入的（没有感叹号）
    'chenxi123',   # 默认的（有感叹号）
    'admin',
    '123456',
    '',             # 空密码
]

print("测试密码匹配：")
print("=" * 50)
for pwd in test_passwords:
    result = bcrypt.checkpw(pwd.encode('utf-8'), hashed.encode('utf-8'))
    status = "✓ 匹配" if result else "✗ 不匹配"
    display = f"'{pwd}'" if pwd else "'' (空字符串)"
    print(f"  {display}: {status}")

print("=" * 50)
print(f"\n数据库中的哈希: {hashed[:30]}...")
