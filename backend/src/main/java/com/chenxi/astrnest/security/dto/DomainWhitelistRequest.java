package com.chenxi.astrnest.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DomainWhitelistRequest(
    @NotBlank @Size(max = 200) String domain,
    @Size(max = 255) String remark
) {}
