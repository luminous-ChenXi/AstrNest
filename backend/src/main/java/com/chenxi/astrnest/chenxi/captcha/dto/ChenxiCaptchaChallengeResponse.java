package com.chenxi.astrnest.chenxi.captcha.dto;

public record ChenxiCaptchaChallengeResponse(
    String captchaId,
    String imageBase64,
    int width,
    int height,
    long expiresIn
) {
}
