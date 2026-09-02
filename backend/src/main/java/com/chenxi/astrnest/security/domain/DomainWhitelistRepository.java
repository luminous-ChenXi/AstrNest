package com.chenxi.astrnest.security.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainWhitelistRepository extends JpaRepository<DomainWhitelistEntry, Long> {
  Optional<DomainWhitelistEntry> findByDomain(String domain);
}
