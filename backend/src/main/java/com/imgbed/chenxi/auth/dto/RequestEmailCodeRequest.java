package com.imgbed.chenxi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestEmailCodeRequest(
    @NotBlank @Email String email,
    @NotBlank String captchaToken
) {
}
