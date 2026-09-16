package com.chenxi.astrnest.install;

/**
 * 安装向导单条环境检测项。
 *
 * @param id      检测项标识（database / schema / storage / java / ffmpeg）
 * @param name    展示名称
 * @param passed  是否通过
 * @param warning 是否为"非阻断问题"（未通过但不影响继续安装，如 ffmpeg 缺失）
 * @param detail  人读详情（含失败排查提示）
 */
public record InstallCheckItem(String id, String name, boolean passed, boolean warning, String detail) {
}
