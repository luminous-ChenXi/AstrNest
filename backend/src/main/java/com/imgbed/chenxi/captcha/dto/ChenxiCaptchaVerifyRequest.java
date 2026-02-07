package com.imgbed.chenxi.captcha.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChenxiCaptchaVerifyRequest(
    @NotBlank String captchaId,
    @NotBlank String captchaCode
) {
}
