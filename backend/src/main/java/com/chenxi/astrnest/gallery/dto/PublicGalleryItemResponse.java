package com.chenxi.astrnest.gallery.dto;

import com.chenxi.astrnest.tag.dto.ChenxiTagResponse;
import java.time.Instant;
import java.util.List;

public record PublicGalleryItemResponse(
    Long id,
    String fileName,
    String publicUrl,
    String thumbnailUrl,
    String mediaCategory,
    String objectKey,
    long size,
    Instant uploadedAt,
    Long ownerId,
    String ownerDisplayName,
    String ownerAvatarUrl,
    long likeCount,
    long invokeCount,
    boolean publicAccessible,
    boolean likedByMe,
    List<ChenxiTagResponse> tags,
    PublicRecentLikeResponse latestLike,
    PublicGalleryAlbumInfo album
) {
  public record PublicGalleryAlbumInfo(
      Long id,
      String title,
      String pathSlug
  ) {}
}
