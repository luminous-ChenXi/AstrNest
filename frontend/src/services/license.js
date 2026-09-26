import http from './http'

/**
 * 正版授权状态（chenxi.license.*，N2 占位，默认关闭）。
 *
 * GET /api/license/status → { enabled, state, needsBanner, lastVerifiedAt, expiresAt, message }
 * 默认关闭时 enabled=false，前端不渲染任何授权 UI；
 * needsBanner=true（超离线宽限/授权无效）时仅展示横幅提醒——绝不锁数据。
 */
export const fetchLicenseStatus = async () => {
  const { data } = await http.get('/api/license/status', { timeout: 10000 })
  return data
}
