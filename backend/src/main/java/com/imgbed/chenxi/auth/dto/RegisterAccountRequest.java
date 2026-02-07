package com.imgbed.chenxi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAccountRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, max = 6) String code,
    @NotBlank @Size(min = 4, max = 32) String username,
    @NotBlank @Size(min = 8, max = 64) String password,
    @Size(max = 64) String displayName
) {
}
