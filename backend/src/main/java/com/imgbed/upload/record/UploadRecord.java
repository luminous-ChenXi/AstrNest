package com.imgbed.upload.record;

import com.imgbed.security.apikey.ApiKey;
import com.imgbed.security.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
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

  @Column(name = "storage_path", nullable = false, length = 255)
  private String objectKey;

  @Column(name = "image_link", nullable = false, length = 255)
  private String publicUrl;

  @Column(name = "file_name", nullable = false, length = 180)
  private String fileName;

  @Column(length = 120)
  private String contentType;

  @Column(nullable = false)
  private long size;

  @Column(length = 40)
  private String reviewStatus;

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

  @Column(name = "uploader_ip", length = 64)
  private String uploaderIp;

  @Column(name = "storage_full_path", length = 512)
  private String storageFullPath;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "api_key_id")
  private ApiKey apiKey;

  @Column(nullable = false, updatable = false)
  private Instant uploadedAt = Instant.now();
}
