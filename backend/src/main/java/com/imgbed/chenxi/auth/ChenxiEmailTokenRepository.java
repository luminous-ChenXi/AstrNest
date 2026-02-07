package com.imgbed.chenxi.auth;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChenxiEmailTokenRepository extends JpaRepository<ChenxiEmailToken, Long> {

  Optional<ChenxiEmailToken> findTopByEmailAndSceneOrderByCreatedAtDesc(String email, ChenxiEmailScene scene);

  Optional<ChenxiEmailToken> findTopByEmailAndSceneAndConsumedFalseOrderByCreatedAtDesc(String email, ChenxiEmailScene scene);

  long countByEmailAndSceneAndCreatedAtAfter(String email, ChenxiEmailScene scene, Instant after);
}
