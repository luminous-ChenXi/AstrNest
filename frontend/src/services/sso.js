import http from './http'

/**
 * SSO（外部身份源/辰汐通行证）登录前端辅助。配置命名空间：chenxi.passport.*（默认关闭）。
 *
 * 兼容任何 OAuth 2.1 / OIDC「授权码 + PKCE(S256)」的 public client 身份源。
 *
 * 流程：
 *   1. fetchSsoConfig 拉取本地配置（enabled/issuer/端点等）；
 *   2. createPkce 生成 code_verifier / code_challenge，连同 state/nonce 存 sessionStorage；
 *   3. buildAuthorizeUrl 跳转身份源授权页；
 *   4. 回调页把 code + codeVerifier 提交本地 /api/auth/sso/exchange（服务端完成换 token），
 *      换取与本地登录一致的本地 JWT。
 */

// sessionStorage 键名：回调页与登录发起页共享
export const SSO_SESSION_KEYS = {
  state: 'astrnest_sso_state',
  nonce: 'astrnest_sso_nonce',
  verifier: 'astrnest_sso_verifier',
  returnTo: 'astrnest_sso_return_to',
}

/** 拉取本地 SSO 配置；未启用时返回 { enabled: false }。 */
export const fetchSsoConfig = () => http.get('/api/auth/sso/config')

// base64url 编码（PKCE 与随机串共用，无填充符）
const base64UrlEncode = (bytes) => {
  let binary = ''
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

/** 随机 base64url 字符串（cryptographically secure），用于 state/nonce。 */
export const randomToken = (byteLength = 24) => {
  const bytes = new Uint8Array(byteLength)
  crypto.getRandomValues(bytes)
  return base64UrlEncode(bytes)
}

/**
 * 生成 PKCE 参数：code_verifier（64 字符，满足 RFC 7636 的 43~128 长度要求）与
 * code_challenge = base64url(SHA256(code_verifier))。
 */
export const createPkce = async () => {
  const codeVerifier = randomToken(48)
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(codeVerifier))
  const codeChallenge = base64UrlEncode(new Uint8Array(digest))
  return { codeVerifier, codeChallenge }
}

/** 拼接身份源授权地址（response_type=code + PKCE S256 + state + nonce）。 */
export const buildAuthorizeUrl = (cfg, { state, nonce, codeChallenge }) => {
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: cfg.clientId,
    redirect_uri: cfg.redirectUri,
    scope: (cfg.scopes || []).join(' '),
    state,
    nonce,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256',
  })
  return `${cfg.authorizeEndpoint}?${params.toString()}`
}

/**
 * 授权码换登录态（统一走本地后端，服务端交换）：
 *   POST /api/auth/sso/exchange { code, codeVerifier }
 *   后端向 {issuer}/oauth2/token 完成 PKCE 换取（public client，无 client_secret）、
 *   回站自省 + userinfo + 影子账号，返回与本地登录一致的 { token, profile, tokenType, expiresIn }。
 *
 * 与旧版差异：浏览器不再直接请求身份源 token 端点（免除身份源 CORS 依赖），
 * 通行证 access_token 全程不出现在浏览器。
 */
export const exchangeCode = async (_cfg, { code, codeVerifier }) => {
  const { data } = await http.post('/api/auth/sso/exchange', { code, codeVerifier })
  return data
}

/** 清理 SSO 流程的 sessionStorage 残留（回调页处理完成后调用，保证一次性）。 */
export const clearSsoSession = () => {
  Object.values(SSO_SESSION_KEYS).forEach((key) => {
    try {
      sessionStorage.removeItem(key)
    } catch {
      /* 忽略隐私模式等存储异常 */
    }
  })
}
