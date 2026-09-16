package com.chenxi.astrnest.album;

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
@Table(name = "album_access_logs")
@Getter
@Setter
public class AlbumAccessLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "album_id", nullable = false)
  private Album album;

  @Column(name = "media_uuid", length = 36)
  private String mediaUuid;

  @Column(name = "client_ip", length = 64)
  private String clientIp;

  @Column(name = "user_agent", length = 512)
  private String userAgent;

  @Column(length = 512)
  private String referer;

  @Column(name = "accessed_at", nullable = false)
  private Instant accessedAt;

  @PrePersist
  public void prePersist() {
    if (this.accessedAt == null) {
      this.accessedAt = Instant.now();
    }
  }
}
