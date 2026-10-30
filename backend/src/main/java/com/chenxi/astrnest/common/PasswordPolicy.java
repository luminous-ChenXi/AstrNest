package com.chenxi.astrnest.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 统一密码策略（审计 P2-9）：此前各处口径不一——安装向导要求「8-64 位且含字母和数字」，
 * 注册/邮箱找回重置只要 8 位，改密连长度下限都没有。现取现状最严口径（安装向导规则）
 * 归一为一份实现，注册、改密、找回重置与安装向导共用，不再各自为政。
 */
public final class PasswordPolicy {

  private PasswordPolicy() {
  }

  /** 校验失败抛 400；文案沿用安装向导原文，前端提示不变 */
  public static void validate(String password) {
    if (password == null || password.length() < 8 || password.length() > 64) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码长度需在 8-64 位之间");
    }
    boolean hasLetter = password.matches(".*[A-Za-z].*");
    boolean hasDigit = password.matches(".*\\d.*");
    if (!hasLetter || !hasDigit) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码需同时包含字母和数字");
    }
  }
}
