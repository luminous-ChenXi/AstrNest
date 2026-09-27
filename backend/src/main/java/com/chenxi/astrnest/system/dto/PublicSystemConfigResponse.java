package com.chenxi.astrnest.system.dto;

public record PublicSystemConfigResponse(
    String customFooterHtml,
    Integer autoCleanupDays,
    String assetDomain,
    Integer maxFilesPerUpload,
    Integer maxUploadMegabytes,
    Integer maxVideoUploadMegabytes,
    Boolean videoChunkUploadEnabled,
    Integer videoChunkSizeMb,
    Boolean guestUploadEnabled,
    /** 是否开放邮箱注册（注册页据此提示） */
    Boolean registrationEnabled,
    /** 注册是否必须邮箱验证（注册页据此决定「验证码两步流」或「直接注册」） */
    Boolean emailVerifyRequired,
    /** 是否已完成安装（false 时其余字段为 null，schema 未安装时不允许触碰 system_config 表） */
    boolean installed
) {

  /**
   * 未安装时的最小响应：schema 尚未就绪时任何 JPA 查询都会 500，
   * 前端靠 installed=false 直接进入安装向导。
   */
  public static PublicSystemConfigResponse uninstalled() {
    return new PublicSystemConfigResponse(null, null, null, null, null, null, null, null, null, null, null, false);
  }
}
