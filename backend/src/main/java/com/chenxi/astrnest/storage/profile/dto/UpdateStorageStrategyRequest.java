package com.chenxi.astrnest.storage.profile.dto;

import com.chenxi.astrnest.storage.StorageStrategy;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record UpdateStorageStrategyRequest(
    @NotBlank(message = "展示名不能为空") String displayName,
    String description,
    StorageStrategy strategy,
    Map<String, Object> config,
    Boolean active
) {}
