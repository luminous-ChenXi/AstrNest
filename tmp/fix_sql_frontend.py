import io
import re

# ---------- SQL: remove interactions remnants ----------
p = 'backend/db/install-schema.sql'
s = io.open(p, encoding='utf-8').read()
start = s.index('-- 【已废弃】用户互动表')
end = s.index(') ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;', start)
end = s.index('\n', end) + 1
s = s[:start] + '-- interactions 表已随 v1.3 清理移除（评论/互动功能从未实装，点赞走 upload_likes）。' + s[end:]
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('ok', p)

p = 'backend/db/init.sql'
s = io.open(p, encoding='utf-8').read()
start = s.index('CREATE TABLE IF NOT EXISTS interactions (')
# 迁移块以连续的 PREPARE/EXECUTE/DEALLOCATE 段收尾，找到 interactions 段之后的第一个非 interactions 语句边界：
# 从建表开始，吞掉到文件中下一个顶层 "-- ----" 分隔注释或 "CREATE TABLE" 为止之前的 interactions 专属语句
tail = s[start:]
m = re.search(r"\n-- ={5,}.*\n|\nCREATE TABLE IF NOT EXISTS (?!interactions)", tail[10:])
if m:
    end = start + 10 + m.start() + 1
else:
    end = len(s)
block = s[start:end]
assert 'interactions' in block and 'DEALLOCATE' in block
s = s[:start] + '-- interactions 表已随 v1.3 清理移除（评论/互动功能从未实装，点赞走 upload_likes）。' + s[end:]
# email 漂移：实体可空且注册允许无邮箱，init.sql 却 NOT NULL
s = s.replace("email VARCHAR(180) NOT NULL COMMENT '邮箱',", "email VARCHAR(180) NULL COMMENT '邮箱',", 1)
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('ok', p, 'removed block chars:', len(block))

p = 'backend/db/init_windows.sql'
s = io.open(p, encoding='utf-8').read()
start = s.index('CREATE TABLE IF NOT EXISTS interactions (')
tail = s[start:]
m = re.search(r"\n-- ={5,}.*\n|\nCREATE TABLE IF NOT EXISTS (?!interactions)", tail[10:])
if m:
    end = start + 10 + m.start() + 1
else:
    end = len(s)
block = s[start:end]
assert 'interactions' in block and 'DEALLOCATE' in block
s = s[:start] + '-- interactions 表已随 v1.3 清理移除（评论/互动功能从未实装，点赞走 upload_likes）。' + s[end:]
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('ok', p, 'removed block chars:', len(block))

# ---------- Frontend ----------
p = 'frontend/src/views/auth/RegisterView.vue'
s = io.open(p, encoding='utf-8').read()
s = s.replace(
    "const usernamePattern = /^[A-Za-z0-9_.-]{1,20}$/",
    "const usernamePattern = /^[A-Za-z0-9_.-]{4,32}$/")
s = s.replace(
    "  if (trimmed.length > 20) return callback(new Error('用户名需在 20 个字符以内'))",
    "  if (trimmed.length < 4) return callback(new Error('用户名至少 4 个字符'))\n"
    "  if (trimmed.length > 32) return callback(new Error('用户名需在 32 个字符以内'))")
s = s.replace(
    "  await sendCode({ email: form.email, captchaToken: form.captchaToken })",
    "  await sendCode({ email: form.email, captchaToken: form.captchaToken })\n"
    "  // 认证令牌一次性消费：发送成功即失效，重发需重新完成人机验证\n"
    "  form.captchaToken = ''")
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('ok', p)

p = 'frontend/src/composables/useChenxiEmailCode.js'
s = io.open(p, encoding='utf-8').read()
s = s.replace(
    "ElMessage.success('验证码已发送到邮箱，请在 5 分钟内完成验证')",
    "// 有效期按场景不同（注册 30 分钟 / 找回 5 分钟），文案不再写死\n"
    "      ElMessage.success('验证码已发送，请查收邮箱')")
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('ok', p)

p = 'frontend/src/services/chenxi.js'
s = io.open(p, encoding='utf-8').read()
s = s.replace(
    "export const checkEmailAvailability = (email) => http.get(`/api/auth/chenxi/check-email?email=${email}`)",
    "export const checkEmailAvailability = (email) => http.get(`/api/auth/chenxi/check-email?email=${encodeURIComponent(email)}`)")
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('ok', p)

# stores/auth.js: expiresIn 优先于写死的 30 天
p = 'frontend/src/stores/auth.js'
s = io.open(p, encoding='utf-8').read()
s = s.replace(
    "const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000",
    "// 兜底 TTL：后端下发 expiresIn（秒）时优先使用，避免后端调小 access-token-days 后前端仍按 30 天显示已登录\n"
    "const SESSION_TTL_MS = 30 * 24 * 60 * 60 * 1000")
s = s.replace(
    "    setSession(token, profile, tokenType) {",
    "    setSession(token, profile, tokenType, expiresInSeconds = null) {")
s = s.replace(
    "      const expiresAt = Date.now() + SESSION_TTL_MS\n",
    "      const expiresAt = Date.now()\n"
    "        + (Number.isFinite(expiresInSeconds) && expiresInSeconds > 0 ? expiresInSeconds * 1000 : SESSION_TTL_MS)\n", 1)
s = s.replace(
    "      const expiresAt = Date.now() + SESSION_TTL_MS\n",
    "      const expiresAt = Date.now()\n"
    "        + (Number.isFinite(expiresInSeconds) && expiresInSeconds > 0 ? expiresInSeconds * 1000 : SESSION_TTL_MS)\n", 1)
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('ok', p)
