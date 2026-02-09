package com.chenxi.astrnest.album;

import com.chenxi.astrnest.security.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "albums")
@Getter
@Setter
public class Album {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "album_uuid", nullable = false, unique = true, length = 36, columnDefinition = "CHAR(36)")
  private String albumUuid;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(name = "path_slug", nullable = false, unique = true, length = 50)
  private String pathSlug;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "is_public", nullable = false)
  private boolean isPublic = false;

  @Column(name = "cover_image_uuid", length = 36)
  private String coverImageUuid;

  @Column(name = "access_count", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
  private Long accessCount = 0L;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "album", fetch = FetchType.LAZY)
  private List<AlbumMedia> albumMedias = new ArrayList<>();

  @PrePersist
  public void prePersist() {
    if (this.albumUuid == null) {
      this.albumUuid = UUID.randomUUID().toString();
    }
    if (this.createdAt == null) {
      this.createdAt = Instant.now();
    }
    if (this.updatedAt == null) {
      this.updatedAt = Instant.now();
    }
    if (this.accessCount == null) {
      this.accessCount = 0L;
    }
  }
}
