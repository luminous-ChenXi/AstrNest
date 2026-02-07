package com.imgbed.admin.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequest(@NotBlank String role) {}
