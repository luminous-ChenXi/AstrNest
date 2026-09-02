package com.chenxi.astrnest.tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "tags")
@Getter
@Setter
public class ChenxiTag {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120, unique = true)
  private String name;

  @Column(length = 180, unique = true)
  private String slug;

  @Column(length = 255)
  private String description;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
    if (!StringUtils.hasText(this.slug)) {
      this.slug = generateSlug(this.name);
    }
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
    if (!StringUtils.hasText(this.slug)) {
      this.slug = generateSlug(this.name);
    }
  }

  private String generateSlug(String source) {
    if (!StringUtils.hasText(source)) {
      return null;
    }
    String ascii = Normalizer.normalize(source, Normalizer.Form.NFD)
        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("-+", "-")
        .replaceAll("^-|-$", "");
    if (StringUtils.hasText(ascii)) {
      return ascii;
    }
    String fallbackSource = source.trim().toLowerCase(Locale.ROOT);
    byte[] hashBytes = Objects.requireNonNull(fallbackSource.getBytes(StandardCharsets.UTF_8));
    String hash = DigestUtils.md5DigestAsHex(hashBytes);
    return "tag-" + hash.substring(0, Math.min(12, hash.length()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ChenxiTag chenxiTag)) {
      return false;
    }
    return id != null && Objects.equals(id, chenxiTag.id);
  }

  @Override
  public int hashCode() {
    return 31;
  }
}
