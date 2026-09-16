package com.chenxi.astrnest.upload;

import com.chenxi.astrnest.system.SystemConfigService;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 访客上传服务 - 管理未登录用户的上传配额和限制
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuestUploadService {

  private final UploadRecordRepository uploadRecordRepository;
  private final SystemConfigService systemConfigService;

  // IP 上传计数缓存：IP -> 今日上传数量
  private final Map<String, Integer> ipUploadCountCache = new ConcurrentHashMap<>();
  // IP 最后上传时间缓存：IP -> 最后上传时间
  private final Map<String, Instant> ipLastUploadTimeCache = new ConcurrentHashMap<>();

  // 缓存清理间隔（分钟）
  private static final int CACHE_CLEANUP_INTERVAL_MINUTES = 60;
  private Instant lastCacheCleanup = Instant.now();

  /**
   * 检查访客上传权限
   *
   * @param ipAddress 访客 IP 地址
   */
  public void checkGuestUploadPermission(String ipAddress) {
    if (!systemConfigService.isGuestUploadEnabled()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "未登录用户不允许上传，请先登录");
    }

    // 清理过期缓存
    cleanupExpiredCache();

    // 获取访客每日上传限制（使用系统配置的 20% 作为访客限制，或固定值）
    int guestDailyLimit = systemConfigService.currentGuestDailyUploadLimit();

    // 检查 IP 今日上传数量
    int uploadedToday = ipUploadCountCache.getOrDefault(ipAddress, 0);

    if (uploadedToday >= guestDailyLimit) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS,
          "今日访客上传次数已达上限（" + guestDailyLimit + "），请登录后上传或明日再试"
      );
    }

    log.debug("访客 {} 今日已上传 {}/{} 个文件", ipAddress, uploadedToday, guestDailyLimit);
  }

  /**
   * 记录访客上传
   *
   * @param ipAddress 访客 IP 地址
   * @param fileCount 上传文件数量
   */
  public void recordGuestUpload(String ipAddress, int fileCount) {
    if (ipAddress == null || ipAddress.isBlank()) {
      return;
    }

    ipUploadCountCache.merge(ipAddress, fileCount, Integer::sum);
    ipLastUploadTimeCache.put(ipAddress, Instant.now());

    log.info("访客 {} 上传了 {} 个文件，今日累计: {}",
        ipAddress, fileCount, ipUploadCountCache.get(ipAddress));
  }

  /**
   * 获取访客今日已上传数量
   *
   * @param ipAddress 访客 IP 地址
   * @return 今日上传数量
   */
  public int getGuestTodayUploadCount(String ipAddress) {
    if (ipAddress == null || ipAddress.isBlank()) {
      return 0;
    }
    return ipUploadCountCache.getOrDefault(ipAddress, 0);
  }

  /**
   * 清理过期缓存（超过24小时的记录）
   */
  private void cleanupExpiredCache() {
    Instant now = Instant.now();

    // 每小时清理一次
    if (now.isBefore(lastCacheCleanup.plus(CACHE_CLEANUP_INTERVAL_MINUTES, ChronoUnit.MINUTES))) {
      return;
    }

    Instant cutoff = now.minus(24, ChronoUnit.HOURS);

    ipLastUploadTimeCache.entrySet().removeIf(entry -> {
      if (entry.getValue().isBefore(cutoff)) {
        ipUploadCountCache.remove(entry.getKey());
        return true;
      }
      return false;
    });

    lastCacheCleanup = now;
    log.debug("清理访客上传缓存完成");
  }

  /**
   * 重置指定 IP 的上传计数（用于管理操作）
   *
   * @param ipAddress IP 地址
   */
  public void resetIpUploadCount(String ipAddress) {
    if (ipAddress != null && !ipAddress.isBlank()) {
      ipUploadCountCache.remove(ipAddress);
      ipLastUploadTimeCache.remove(ipAddress);
      log.info("重置访客 {} 的上传计数", ipAddress);
    }
  }
}
