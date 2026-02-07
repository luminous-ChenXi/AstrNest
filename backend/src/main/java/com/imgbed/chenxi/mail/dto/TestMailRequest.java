package com.imgbed.chenxi.mail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TestMailRequest(@NotBlank @Email String targetEmail) {
}
