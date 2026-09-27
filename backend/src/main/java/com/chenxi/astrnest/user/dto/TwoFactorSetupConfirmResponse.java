package com.chenxi.astrnest.user.dto;

import com.chenxi.astrnest.security.dto.UserProfileResponse;
import java.util.List;

/**
 * POST /api/auth/2fa/setup/confirm 响应：绑定成功即视为登录完成。
 *
 * @param token         正式 JWT
 * @param profile       用户资料
 * @param tokenType     认证头类型（Bearer）
 * @param expiresIn     token 有效期（秒）
 * @param recoveryCodes 10 个 8 位一次性还原码明文——仅此一次展示，请立即保存
 */
public record TwoFactorSetupConfirmResponse(
    String token,
    UserProfileResponse profile,
    String tokenType,
    long expiresIn,
    List<String> recoveryCodes) {
}
