package com.chenxi.astrnest.security.user;

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
@Table(name = "roles")
@Getter
@Setter
public class UserRole {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String name;

  @Column(length = 255)
  private String description;

  /** 列定义与 install-schema.sql 对齐，保证两种建表顺序下结构一致。 */
  @Column(name = "created_at", nullable = false, updatable = false,
      columnDefinition = "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)")
  private Instant createdAt = Instant.now();

  /** 列定义与 install-schema.sql 对齐（DB 默认值 + ON UPDATE）。 */
  @Column(name = "updated_at", nullable = false,
      columnDefinition = "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)")
  private Instant updatedAt = Instant.now();
}
