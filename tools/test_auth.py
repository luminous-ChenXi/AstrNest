#!/usr/bin/env python3
"""模拟 Spring Security 认证流程测试"""

import bcrypt

# 数据库中的密码哈希
stored_hash = '$2b$12$1uLnox51dsclaN4VP7wQnen64wtIuBZyp98vltJgmOgEoTjC6/En2'

# 用户输入的密码
test_password = 'chenxi123'

print("=" * 60)
print("Spring Security 认证流程测试")
print("=" * 60)

# 1. 检查密码是否匹配
print(f"\n1. 测试密码: '{test_password}'")
print(f"   存储的哈希: {stored_hash[:40]}...")

password_bytes = test_password.encode('utf-8')
hash_bytes = stored_hash.encode('utf-8')

is_match = bcrypt.checkpw(password_bytes, hash_bytes)
print(f"   密码匹配结果: {'✓ 成功' if is_match else '✗ 失败'}")

# 2. 检查哈希格式
print(f"\n2. 哈希格式检查:")
print(f"   算法标识: {stored_hash[:4]}")  # $2b$
print(f"   成本因子: {stored_hash[4:6]}")  # 12

# 3. 重新生成哈希对比
print(f"\n3. 重新生成哈希对比:")
new_hash = bcrypt.hashpw(password_bytes, bcrypt.gensalt(rounds=12))
print(f"   新哈希: {new_hash.decode()[:40]}...")
print(f"   注意: 每次生成的哈希都不同，但都应该能验证同一个密码")

# 4. 验证新哈希也能通过
is_new_match = bcrypt.checkpw(password_bytes, new_hash)
print(f"   新哈希验证: {'✓ 成功' if is_new_match else '✗ 失败'}")

print("\n" + "=" * 60)
if is_match:
    print("结论: 密码验证应该通过！")
    print("如果登录仍然失败，问题可能在:")
    print("  1. 前端传递的密码不正确")
    print("  2. 后端有其他验证逻辑")
    print("  3. 数据库连接的不是同一个实例")
else:
    print("结论: 密码不匹配！")
print("=" * 60)
