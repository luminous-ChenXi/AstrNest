package com.chenxi.astrnest.license.dto;

import java.time.Instant;

/**
 * POST verify-url 的请求体（N2 占位，协议只到接口层）。
 * 当前仅携带站点时间戳；站点标识、实例指纹等字段由 N2 阶段的"辰汐授权服务"协议确定后补充。
 */
public record LicenseVerifyRequest(Instant reportedAt) {
}
