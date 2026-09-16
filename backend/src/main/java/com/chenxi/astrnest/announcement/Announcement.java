package com.chenxi.astrnest.announcement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "announcements")
@Getter
@Setter
public class Announcement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 180)
  private String title;

  @Column(length = 360)
  private String summary;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private AnnouncementLevel level = AnnouncementLevel.NOTICE;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private AnnouncementStatus status = AnnouncementStatus.DRAFT;

  @Column(name = "pinned", nullable = false)
  private boolean pinned = false;

  @Lob
  @Column(name = "content_markdown", columnDefinition = "LONGTEXT")
  private String contentMarkdown;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "author", length = 120)
  private String author;

  @Column(name = "author_user_id")
  private Long authorUserId;

  @Column(name = "author_role", length = 120)
  private String authorRole;

  @Column(name = "author_avatar", length = 512)
  private String authorAvatar;

  @Column(name = "updated_by", length = 120)
  private String updatedBy;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
    if (this.status == AnnouncementStatus.PUBLISHED && this.publishedAt == null) {
      this.publishedAt = now;
    }
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
    if (this.status == AnnouncementStatus.PUBLISHED && this.publishedAt == null) {
      this.publishedAt = this.updatedAt;
    }
  }
}
