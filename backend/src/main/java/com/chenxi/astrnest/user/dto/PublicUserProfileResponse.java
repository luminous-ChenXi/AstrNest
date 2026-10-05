package com.chenxi.astrnest.user.dto;

/**
 * 公开用户档案（GET /api/public/users/{id}，匿名可访问）。
 * 邮箱属 PII，任何公开端点不得输出（防止批量收割与账号枚举）。
 */
public record PublicUserProfileResponse(
    Long id,
    String displayName,
    String avatarUrl,
    String signature,
    long uploadCount,
    long storageBytes,
    long likeCount
) {}
