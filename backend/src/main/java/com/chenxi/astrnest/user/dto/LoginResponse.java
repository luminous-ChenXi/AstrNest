package com.chenxi.astrnest.user.dto;

import com.chenxi.astrnest.security.dto.UserProfileResponse;

/**
 * POST /api/auth/login（及 /api/auth/2fa/* 换发端点）响应。
 *
 * <p>三态模式（mode）：</p>
 * <ul>
 *   <li>{@code COMPLETE}：登录完成，token/profile 有效；</li>
 *   <li>{@code TOTP_CHALLENGE}：密码已通过但用户已绑定二步验证——返回 5 分钟过渡令牌，
 *       前端展示动态码输入框后调用 /api/auth/2fa/verify 换正式 JWT（此时 token 为 null）；</li>
 *   <li>{@code TOTP_SETUP}：密码已通过且站长开启了强制二步验证、该用户尚未绑定——
 *       返回过渡令牌 + otpauth URI + 密钥，前端渲染二维码绑定后调用
 *       /api/auth/2fa/setup/confirm（此时 token 为 null）。</li>
 * </ul>
 */
public record LoginResponse(
    String token,
    UserProfileResponse profile,
    String tokenType,
    long expiresIn,
    String mode,
    TwoFactorChallenge twoFactor
) {

  public static final String MODE_COMPLETE = "COMPLETE";
  public static final String MODE_TOTP_CHALLENGE = "TOTP_CHALLENGE";
  public static final String MODE_TOTP_SETUP = "TOTP_SETUP";

  /** 登录完成响应（兼容旧字段顺序：token/profile/tokenType/expiresIn）。 */
  public static LoginResponse complete(String token, UserProfileResponse profile, String tokenType, long expiresIn) {
    return new LoginResponse(token, profile, tokenType, expiresIn, MODE_COMPLETE, null);
  }

  /** 二步验证挑战响应（已绑定用户）：不发 JWT，只发过渡令牌。 */
  public static LoginResponse totpChallenge(String tempToken, String username) {
    return new LoginResponse(null, null, null, 0, MODE_TOTP_CHALLENGE,
        new TwoFactorChallenge(tempToken, null, null, username));
  }

  /** 二步验证强制绑定响应（未绑定用户）：过渡令牌 + otpauth URI + 密钥（用于前端渲染二维码）。 */
  public static LoginResponse totpSetup(String tempToken, String otpauthUri, String secret, String username) {
    return new LoginResponse(null, null, null, 0, MODE_TOTP_SETUP,
        new TwoFactorChallenge(tempToken, otpauthUri, secret, username));
  }
}
