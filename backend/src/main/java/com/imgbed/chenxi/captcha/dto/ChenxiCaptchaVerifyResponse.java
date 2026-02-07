package com.imgbed.chenxi.captcha.dto;

public record ChenxiCaptchaVerifyResponse(boolean passed, String certificationToken) {
}
