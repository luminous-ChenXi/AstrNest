#!/usr/bin/env python3
"""
测试 Spring Security 的 BCrypt 兼容性
Spring Security 使用 $2a$ 格式，而 Python bcrypt 使用 $2b$ 格式
"""

import bcrypt

# 数据库中的密码哈希 (Python bcrypt 生成的 $2b$ 格式)
stored_hash_b = '$2b$12$1uLnox51dsclaN4VP7wQnen64wtIuBZyp98vltJgmOgEoTjC6/En2'

# 转换为 Spring Security 格式 ($2a$)
stored_hash_a = stored_hash_b.replace('$2b$', '$2a$')

password = 'chenxi123'
password_bytes = password.encode('utf-8')

print("=" * 70)
print("BCrypt 格式兼容性测试")
print("=" * 70)

print(f"\n原始哈希 ($2b$ - Python bcrypt 格式):")
print(f"  {stored_hash_b}")

print(f"\n转换后哈希 ($2a$ - Spring Security 格式):")
print(f"  {stored_hash_a}")

print(f"\n测试密码: '{password}'")
print("-" * 70)

# 测试 $2b$ 格式 (Python 默认)
print("\n1. 测试 $2b$ 格式 (Python bcrypt):")
result_b = bcrypt.checkpw(password_bytes, stored_hash_b.encode('utf-8'))
print(f"   结果: {'✓ 匹配' if result_b else '✗ 不匹配'}")

# 测试 $2a$ 格式 (Spring Security)
print("\n2. 测试 $2a$ 格式 (Spring Security):")
try:
    result_a = bcrypt.checkpw(password_bytes, stored_hash_a.encode('utf-8'))
    print(f"   结果: {'✓ 匹配' if result_a else '✗ 不匹配'}")
except Exception as e:
    print(f"   错误: {e}")

# 生成新的哈希对比
print("\n3. 生成新的哈希对比:")
new_hash = bcrypt.hashpw(password_bytes, bcrypt.gensalt(rounds=12))
print(f"   新哈希: {new_hash.decode()}")
print(f"   格式: {new_hash.decode()[:4]}")

# 检查版本差异
print("\n" + "=" * 70)
print("重要发现:")
print("=" * 70)
print("""
Spring Security 的 BCryptPasswordEncoder 默认使用 $2a$ 格式
Python 的 bcrypt 库默认使用 $2b$ 格式

虽然 $2a$ 和 $2b$ 在算法上是兼容的，但某些版本的 Spring Security
可能无法正确验证 $2b$ 格式的哈希。

解决方案:
1. 使用 $2a$ 格式生成密码哈希
2. 或者在 Spring Security 中配置兼容 $2b$ 格式
""")

# 尝试用 $2a$ 格式重新生成哈希
print("\n4. 使用 $2a$ 前缀重新验证:")
# 注意：bcrypt 库可能不接受 $2a$ 格式，但 Spring Security 应该可以
