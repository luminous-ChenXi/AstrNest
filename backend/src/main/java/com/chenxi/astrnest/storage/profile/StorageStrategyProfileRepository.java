package com.chenxi.astrnest.storage.profile;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageStrategyProfileRepository extends JpaRepository<StorageStrategyProfile, Long> {

  boolean existsByName(String name);

  Optional<StorageStrategyProfile> findFirstByActiveTrue();

  List<StorageStrategyProfile> findAllByOrderByActiveDescUpdatedAtDesc();
}
