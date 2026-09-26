package com.chenxi.astrnest.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

/**
 * verify-url 的预期响应（N2 占位，协议只到接口层）。
 *
 * <p>N2 阶段由"辰汐授权服务"确定最终报文；当前仅约定最小字段 {@code valid}，
 * 其余字段未知时忽略（{@code ignoreUnknown}）。任何解析失败按 verify 失败处理，
 * 进入离线宽限逻辑，绝不抛出阻断站点的异常。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LicenseVerifyResult(
    boolean valid,
    String licenseId,
    String plan,
    Instant expiresAt) {

  public static LicenseVerifyResult invalid() {
    return new LicenseVerifyResult(false, null, null, null);
  }
}
