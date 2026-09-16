package com.chenxi.astrnest.security.apikey;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

  Optional<ApiKey> findByPublicId(String publicId);

  Optional<ApiKey> findFirstByOrderByCreatedAtDesc();

  List<ApiKey> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
