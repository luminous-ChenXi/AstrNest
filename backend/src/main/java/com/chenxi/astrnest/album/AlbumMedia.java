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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "album_media")
@Getter
@Setter
public class AlbumMedia {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "album_id", nullable = false)
  private Album album;

  @Column(name = "media_uuid", nullable = false, length = 36, columnDefinition = "CHAR(36)")
  private String mediaUuid;

  @Column(name = "added_at", nullable = false)
  private Instant addedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "added_by")
  private UserAccount addedBy;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @PrePersist
  public void prePersist() {
    if (this.addedAt == null) {
      this.addedAt = Instant.now();
    }
    if (this.sortOrder == null) {
      this.sortOrder = 0;
    }
  }
}
