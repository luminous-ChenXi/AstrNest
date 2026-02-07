package com.imgbed.user.login;

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
@Table(name = "user_login_events")
@Getter
@Setter
public class UserLoginEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  @Column(length = 64)
  private String ipAddress;

  @Column(length = 180)
  private String location;

  @Column(length = 255)
  private String userAgent;

  @Column(nullable = false, updatable = false)
  private Instant occurredAt = Instant.now();
}
