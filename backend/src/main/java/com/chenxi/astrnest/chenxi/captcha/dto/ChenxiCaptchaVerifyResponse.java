package com.chenxi.astrnest.chenxi.captcha.dto;

public record ChenxiCaptchaVerifyResponse(boolean passed, String certificationToken) {
}
