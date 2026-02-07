package com.imgbed.upload.like;

import com.imgbed.security.user.UserAccount;
import com.imgbed.upload.record.UploadRecord;
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
@Table(name = "upload_likes")
@Getter
@Setter
public class UploadLike {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "upload_id")
  private UploadRecord uploadRecord;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = true)
  private UserAccount user;

  @Column(name = "guest_token", length = 64)
  private String guestToken;

  @Column(name = "guest_display_name", length = 60)
  private String guestDisplayName;

  @Column(name = "guest_avatar_url", length = 255)
  private String guestAvatarUrl;

  @Column(name = "liked_as_guest", nullable = false)
  private boolean likedAsGuest = false;

  @Column(nullable = false, updatable = false)
  private Instant likedAt = Instant.now();
}
