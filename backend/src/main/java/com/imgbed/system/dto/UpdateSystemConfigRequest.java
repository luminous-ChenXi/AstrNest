package com.imgbed.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSystemConfigRequest(
    @NotNull(message = "请填写上传大小上限")
    @Min(value = 1, message = "单文件大小至少 1 MB")
    @Max(value = 512, message = "单文件大小不可超过 512 MB")
    Integer maxUploadMb,

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

    @NotBlank(message = "请填写公开访问域名")
    @Size(max = 255, message = "域名长度不可超过 255 字符")
    String assetDomain,

    @Size(max = 4000, message = "自定义页脚内容不可超过 4000 字符")
    String customFooterHtml
) {
}
