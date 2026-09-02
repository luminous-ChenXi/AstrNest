package com.chenxi.astrnest.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "domain_whitelist")
@Getter
@Setter
public class DomainWhitelistEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 200)
  private String domain;

  @Column(length = 255)
  private String remark;

  @Column(nullable = false)
  private boolean approved = true;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();
}
