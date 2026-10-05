package com.chenxi.astrnest.system;

import com.chenxi.astrnest.album.AlbumAccessLogRepository;
import com.chenxi.astrnest.chenxi.auth.ChenxiEmailTokenRepository;
import com.chenxi.astrnest.chenxi.captcha.ChenxiCaptchaTicketRepository;
import com.chenxi.astrnest.common.InMemoryRateLimiter;
import com.chenxi.astrnest.security.bruteforce.SecurityLogEntryRepository;
import com.chenxi.astrnest.upload.record.UploadRecordService;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据保鲜定时任务：验证码票据、邮件验证码、图集访问日志、安全日志此前只增不删，
 * 长期运行无限膨胀（且匿名验证码接口每调用写一行，可被灌爆）。每日凌晨 04:30 执行一轮清理。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemDataHousekeeping {

  private final ChenxiEmailTokenRepository emailTokenRepository;
  private final ChenxiCaptchaTicketRepository captchaTicketRepository;
  private final AlbumAccessLogRepository albumAccessLogRepository;
  private final SecurityLogEntryRepository securityLogEntryRepository;
  private final UploadRecordService uploadRecordService;
  private final SystemConfigService systemConfigService;
  private final InMemoryRateLimiter rateLimiter;

  @Scheduled(cron = "0 30 4 * * *")
  @Transactional
  public void cleanup() {
    Instant now = Instant.now();
    long emailTokens = emailTokenRepository.deleteByExpiresAtBefore(now.minus(1, ChronoUnit.DAYS));
    long captchaTickets = captchaTicketRepository.deleteByExpiresAtBefore(now.minus(1, ChronoUnit.DAYS));
    long accessLogs = albumAccessLogRepository.deleteByAccessedAtBefore(now.minus(30, ChronoUnit.DAYS));
    long securityLogs = securityLogEntryRepository.deleteByCreatedAtBefore(now.minus(90, ChronoUnit.DAYS));
    int expiredUploads = cleanupExpiredUploadRecords(now);
    rateLimiter.evictIdleBefore(now.minus(Duration.ofHours(1)));
    log.info("数据保鲜完成：邮件验证码 {}、验证码票据 {}、图集访问日志 {}、安全日志 {}、过期上传记录 {}",
        emailTokens, captchaTickets, accessLogs, securityLogs, expiredUploads);
  }

  /** autoCleanupDays=0 视为关闭；删除走既有 deleteRecord（连带物理文件与缩略图口径） */
  private int cleanupExpiredUploadRecords(Instant now) {
    int days = systemConfigService.currentAutoCleanupDays();
    if (days <= 0) {
      return 0;
    }
    return uploadRecordService.deleteExpiredRecords(now.minus(days, ChronoUnit.DAYS));
  }
}
