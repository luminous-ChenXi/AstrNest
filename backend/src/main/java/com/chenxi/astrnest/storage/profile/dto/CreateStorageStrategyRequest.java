package com.chenxi.astrnest.storage.profile.dto;

import com.chenxi.astrnest.storage.StorageStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CreateStorageStrategyRequest(
    @NotBlank(message = "策略名称不能为空") String name,
    @NotBlank(message = "展示名不能为空") String displayName,
    String description,
    @NotNull(message = "请选择存储类型") StorageStrategy strategy,
    @NotEmpty(message = "请填写必要的配置项") Map<String, Object> config,
    Boolean active
) {}
