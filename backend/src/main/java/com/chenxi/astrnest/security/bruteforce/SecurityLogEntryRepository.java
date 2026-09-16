package com.chenxi.astrnest.security.bruteforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecurityLogEntryRepository extends JpaRepository<SecurityLogEntry, Long> {

  long countByEventType(String eventType);

  long countByEventTypeAndCreatedAtAfter(String eventType, java.time.Instant after);

  @Query("select e.username as username, count(e) as cnt from SecurityLogEntry e where e.eventType = :eventType and e.username is not null group by e.username order by cnt desc")
  List<Object[]> findTopUsernames(@Param("eventType") String eventType, Pageable pageable);
}
