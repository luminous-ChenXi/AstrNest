package com.chenxi.astrnest.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSystemConfigRequest(
    @NotNull(message = "请填写图片上传大小上限")
    @Min(value = 1, message = "单文件大小至少 1 MB")
    @Max(value = 512, message = "单文件大小不可超过 512 MB")
    Integer maxUploadMb,

    @NotNull(message = "请填写短视频上传大小上限")
    @Min(value = 1, message = "短视频大小至少 1 MB")
    @Max(value = 2048, message = "短视频大小不可超过 2 GB")
    Integer maxVideoUploadMb,

    @NotNull(message = "请设置每日上传次数限制")
    @Min(value = 1, message = "每日上传次数至少 1 次")
    @Max(value = 100000, message = "每日上传次数不可超过 100000 次")
    Integer dailyUploadCountLimit,

    @NotNull(message = "请设置用户空间配额")
    @Min(value = 1, message = "用户空间至少 1 GB")
    @Max(value = 2048, message = "用户空间配额不可超过 2 TB")
    Integer userStorageQuotaGb,

    Boolean registrationEnabled,

    Boolean guestLikeEnabled,

    Boolean guestUploadEnabled,

    @NotNull(message = "请设置单次上传文件数限制")
    @Min(value = 1, message = "单次上传文件数至少 1 个")
    @Max(value = 100, message = "单次上传文件数不可超过 100 个")
    Integer maxFilesPerUpload,

    @NotNull(message = "请设置自动清理天数，可填写 0 表示关闭")
    @Min(value = 0, message = "自动清理天数不能小于 0")
    @Max(value = 365, message = "自动清理天数不可超过 365")
    Integer autoCleanupDays,

    Boolean videoChunkUploadEnabled,

    @NotNull(message = "请设置分片大小")
    @Min(value = 1, message = "分片大小至少 1 MB")
    @Max(value = 512, message = "分片大小不可超过 512 MB")
    Integer videoChunkSizeMb,

    @NotBlank(message = "请填写公开访问域名")
    @Size(max = 255, message = "域名长度不可超过 255 字符")
    String assetDomain,

    @Size(max = 4000, message = "自定义页脚内容不可超过 4000 字符")
    String customFooterHtml,

    Boolean aiModerationEnabled,

    Boolean aiLabelingEnabled,

    @Size(max = 128, message = "腾讯云 SecretId 长度不可超过 128 字符")
    String aiTencentSecretId,

    @Size(max = 128, message = "腾讯云 SecretKey 长度不可超过 128 字符")
    String aiTencentSecretKey,

    @Size(max = 64, message = "地区标识长度不可超过 64 字符")
    String aiTencentRegion,

    @Size(max = 128, message = "存储桶名称长度不可超过 128 字符")
    String aiTencentBucket,

    @Size(max = 128, message = "识别场景参数长度不可超过 128 字符")
    String aiTencentDetectScenes,

    @Min(value = 0, message = "AI 违规拦截阈值至少为 0")
    @Max(value = 100, message = "AI 违规拦截阈值不可超过 100")
    Integer aiModerationBlockConfidence,

    @Min(value = 0, message = "AI 复核阈值至少为 0")
    @Max(value = 100, message = "AI 复核阈值不可超过 100")
    Integer aiModerationReviewConfidence,

    @Min(value = 0, message = "AI 标签置信度至少为 0")
    @Max(value = 100, message = "AI 标签置信度不可超过 100")
    Integer aiLabelMinConfidence
) {
}
