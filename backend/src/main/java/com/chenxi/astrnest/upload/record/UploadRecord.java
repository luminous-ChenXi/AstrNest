package com.chenxi.astrnest.upload.record;

import com.chenxi.astrnest.ai.AiDecision;
import com.chenxi.astrnest.security.apikey.ApiKey;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.tag.ChenxiTag;
import com.chenxi.astrnest.upload.media.MediaCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "upload_records")
@Getter
@Setter
public class UploadRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "media_uuid", nullable = false, unique = true, length = 36, columnDefinition = "CHAR(36)")
  private String mediaUuid = UUID.randomUUID().toString();

  @Column(name = "storage_path", nullable = false, length = 255)
  private String objectKey;

  @Column(name = "image_link", nullable = false, length = 255)
  private String publicUrl;

  @Column(name = "file_name", nullable = false, length = 180)
  private String fileName;

  @Column(length = 120)
  private String contentType;

  @Enumerated(EnumType.STRING)
  @Column(name = "media_type", length = 16, nullable = false)
  private MediaCategory mediaCategory = MediaCategory.IMAGE;

  @Column(nullable = false)
  private long size;

  @Column(length = 40)
  private String reviewStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "ai_decision", length = 16)
  private AiDecision aiDecision;

  @Column(name = "ai_label_snapshot", columnDefinition = "LONGTEXT")
  private String aiLabelSnapshot;

  @Column(name = "ai_error_code", length = 64)
  private String aiErrorCode;

  @Column(name = "ai_error_message", length = 255)
  private String aiErrorMessage;

  @Column(name = "ai_request_id", length = 128)
  private String aiRequestId;

  @Column(name = "storage_provider", length = 40)
  private String storageProvider = "LOCAL_DISK";

  @Column(name = "storage_mode", length = 40)
  private String storageMode = "PUBLIC";

  @Column(name = "is_violation", nullable = false)
  private boolean violation = false;

  @Column(name = "is_public", nullable = false)
  private boolean publicAccessible = true;

  @Column(name = "like_count", nullable = false)
  private long likeCount = 0L;

  @Column(name = "invoke_count", nullable = false)
  private long invokeCount = 0L;

  @Column(name = "last_access_at")
  private Instant lastAccessAt;

  @Column(name = "uploader_ip", length = 64)
  private String uploaderIp;

  @Column(name = "storage_full_path", length = 512)
  private String storageFullPath;

  @Column(name = "thumbnail_url", length = 500)
  private String thumbnailUrl;

  @Column(name = "thumbnail_storage_path", length = 255)
  private String thumbnailStoragePath;

  @Column(name = "duration_seconds")
  private Integer durationSeconds;

  @Column(name = "embed_url", length = 512)
  private String embedUrl;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "upload_record_tags",
      joinColumns = @JoinColumn(name = "upload_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id")
  )
  private Set<ChenxiTag> tags = new LinkedHashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "api_key_id")
  private ApiKey apiKey;

  @Column(nullable = false, updatable = false)
  private Instant uploadedAt = Instant.now();
}
