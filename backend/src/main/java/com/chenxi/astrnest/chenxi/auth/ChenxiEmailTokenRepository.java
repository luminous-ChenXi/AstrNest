package com.chenxi.astrnest.chenxi.auth;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChenxiEmailTokenRepository extends JpaRepository<ChenxiEmailToken, Long> {

  Optional<ChenxiEmailToken> findTopByEmailAndSceneOrderByCreatedAtDesc(String email, ChenxiEmailScene scene);

  Optional<ChenxiEmailToken> findTopByEmailAndSceneAndConsumedFalseOrderByCreatedAtDesc(String email, ChenxiEmailScene scene);

  long countByEmailAndSceneAndCreatedAtAfter(String email, ChenxiEmailScene scene, Instant after);

  Optional<ChenxiEmailToken> findTopByLinkTokenAndConsumedFalseOrderByCreatedAtDesc(String linkToken);

  /** 保鲜清理：删除过期验证码记录（码只存哈希，过期后无任何价值） */
  long deleteByExpiresAtBefore(java.time.Instant threshold);
}
