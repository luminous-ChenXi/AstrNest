package com.chenxi.astrnest.system.dto;

public record PublicSystemConfigResponse(
    String customFooterHtml,
    Integer autoCleanupDays,
    String assetDomain,
    Integer maxFilesPerUpload,
    Integer maxUploadMegabytes,
    Integer maxVideoUploadMegabytes,
    Boolean videoChunkUploadEnabled,
    Integer videoChunkSizeMb
) {
}
