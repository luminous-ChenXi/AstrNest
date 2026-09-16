package com.chenxi.astrnest.chenxi.captcha.dto;

import jakarta.validation.constraints.NotBlank;

public record ChenxiCaptchaVerifyRequest(
    @NotBlank String captchaId,
    @NotBlank String captchaCode
) {
}
