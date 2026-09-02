package com.chenxi.astrnest.monitoring;

import com.chenxi.astrnest.monitoring.dto.RiskIp;
import com.chenxi.astrnest.monitoring.dto.SecuritySummaryResponse;
import com.chenxi.astrnest.monitoring.dto.TopCount;
import com.chenxi.astrnest.security.bruteforce.AuthLockState;
import com.chenxi.astrnest.security.bruteforce.AuthLockStateRepository;
import com.chenxi.astrnest.security.bruteforce.SecurityLogEntryRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SecurityAnalyticsService {

  private final SecurityLogEntryRepository logRepository;
  private final AuthLockStateRepository lockStateRepository;

  public SecurityAnalyticsService(SecurityLogEntryRepository logRepository, AuthLockStateRepository lockStateRepository) {
    this.logRepository = logRepository;
    this.lockStateRepository = lockStateRepository;
  }

  public SecuritySummaryResponse summarize() {
    Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
    long login24h = logRepository.countByEventTypeAndCreatedAtAfter("LOGIN_FAIL", since24h);
    long loginTotal = logRepository.countByEventType("LOGIN_FAIL");
    long register24h = logRepository.countByEventTypeAndCreatedAtAfter("REGISTER_FAIL", since24h);

    List<TopCount> topLoginUsers = topUsernames("LOGIN_FAIL", 5);
    List<TopCount> topRegisterUsers = topUsernames("REGISTER_FAIL", 5);
    List<RiskIp> riskIps = lockStateRepository.findByLockedUntilAfter(Instant.now()).stream()
        .sorted((a, b) -> b.getLockedUntil().compareTo(a.getLockedUntil()))
        .limit(20)
        .map(this::toRiskIp)
        .toList();

    return new SecuritySummaryResponse(login24h, loginTotal, register24h, topLoginUsers, topRegisterUsers, riskIps);
  }

  private List<TopCount> topUsernames(String eventType, int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    return logRepository.findTopUsernames(eventType, pageable).stream()
        .map(row -> new TopCount((String) row[0], (Long) row[1]))
        .toList();
  }

  private RiskIp toRiskIp(AuthLockState state) {
    return new RiskIp(state.getIp(), state.getLockReason(), state.getLockedUntil(), state.getDimension().name());
  }
}
