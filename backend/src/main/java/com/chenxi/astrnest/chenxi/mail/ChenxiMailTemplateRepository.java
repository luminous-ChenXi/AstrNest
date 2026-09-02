package com.chenxi.astrnest.chenxi.mail;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChenxiMailTemplateRepository extends JpaRepository<ChenxiMailTemplate, Long> {

  boolean existsByType(String type);
}
