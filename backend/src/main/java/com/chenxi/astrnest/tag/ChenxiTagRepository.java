package com.chenxi.astrnest.tag;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChenxiTagRepository extends JpaRepository<ChenxiTag, Long> {

  Optional<ChenxiTag> findByNameIgnoreCase(String name);

  @Query("select t from ChenxiTag t where lower(t.name) in :normalizedNames")
  List<ChenxiTag> findByNormalizedNames(@Param("normalizedNames") Collection<String> normalizedNames);

  List<ChenxiTag> findTop50ByNameContainingIgnoreCaseOrderByNameAsc(String keyword);

  List<ChenxiTag> findTop50ByOrderByCreatedAtDesc();
}
