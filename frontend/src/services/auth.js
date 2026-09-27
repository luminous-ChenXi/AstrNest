import http from './http'

export const login = (payload) => http.post('/api/auth/login', payload)

/**
 * 二步验证挑战：6 位 TOTP 动态码或 8 位一次性还原码 → 换发正式 JWT。
 * tempToken 为登录时下发的 5 分钟过渡令牌。
 */
export const verifyTwoFactor = (payload) => http.post('/api/auth/2fa/verify', payload)

/**
 * 强制绑定确认：输 6 位动态码完成 TOTP 绑定 → 返回一次性还原码（仅此一次展示）+ 正式 JWT。
 */
export const confirmTwoFactorSetup = (payload) => http.post('/api/auth/2fa/setup/confirm', payload)
