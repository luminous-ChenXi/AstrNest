package com.imgbed.user.dto;

import jakarta.validation.constraints.NotEmpty;

public record DeleteUploadsRequest(@NotEmpty(message = "请选择要删除的图片") long[] ids) {}
