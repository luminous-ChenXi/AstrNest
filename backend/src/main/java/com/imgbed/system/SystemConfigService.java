package com.imgbed.system;

import com.imgbed.security.user.UserAccountRepository;
import com.imgbed.system.dto.PublicSystemConfigResponse;
import com.imgbed.system.dto.SystemConfigResponse;
import com.imgbed.system.dto.SystemInsightResponse;
import com.imgbed.system.dto.UpdateSystemConfigRequest;
import com.imgbed.upload.record.UploadRecordRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

  private static final long BYTES_PER_MB = 1024L * 1024L;
  private static final long BYTES_PER_GB = BYTES_PER_MB * 1024L;
  private static final String DEFAULT_ASSET_DOMAIN = "http://localhost:8080";

  private final SystemConfigRepository systemConfigRepository;
  private final UserAccountRepository userAccountRepository;
  private final UploadRecordRepository uploadRecordRepository;

  public SystemConfigResponse getCurrentConfig() {
    return toResponse(loadConfig());
  }

  public PublicSystemConfigResponse getPublicConfig() {
    SystemConfig config = loadConfig();
    return new PublicSystemConfigResponse(config.getCustomFooterHtml());
  }

  @Transactional
  public SystemConfigResponse updateConfig(UpdateSystemConfigRequest request, Authentication authentication) {
    SystemConfig config = loadConfig();
    config.setMaxUploadBytes(request.maxUploadMb() * BYTES_PER_MB);
    config.setDailyUploadCountLimit(request.dailyUploadCountLimit());
    config.setUserStorageQuotaBytes(request.userStorageQuotaGb() * BYTES_PER_GB);
    config.setRegistrationEnabled(Boolean.TRUE.equals(request.registrationEnabled()));
    config.setGuestLikeEnabled(request.guestLikeEnabled() == null ? config.isGuestLikeEnabled() : request.guestLikeEnabled());
    config.setAssetDomain(normalizeDomain(request.assetDomain()));
    config.setCustomFooterHtml(normalizeFooterHtml(request.customFooterHtml()));
    config.setUpdatedBy(resolveUsername(authentication));
    SystemConfig saved = systemConfigRepository.save(config);
    return toResponse(saved);
  }

  public long currentMaxUploadBytes() {
    return loadConfig().getMaxUploadBytes();
  }

  public String currentAssetDomain() {
    return loadConfig().getAssetDomain();
  }

  public boolean isRegistrationEnabled() {
    return loadConfig().isRegistrationEnabled();
  }

  public boolean isGuestLikeEnabled() {
    return loadConfig().isGuestLikeEnabled();
  }

  public SystemInsightResponse getInsights() {
    long totalUsers = userAccountRepository.count();
    long usersWithEmail = userAccountRepository.countUsersWithEmailBound();
    long adminUsers = userAccountRepository.countUsersByRole("ADMIN");
    long totalUploads = uploadRecordRepository.count();
    long todayUploads = uploadRecordRepository.countUploadedAfter(startOfToday());
    long totalStorageBytes = uploadRecordRepository.totalStorageBytes();
    double totalStorageGigabytes = bytesToGigabytes(totalStorageBytes);
    double emailCompletionRate = totalUsers == 0 ? 0d : (double) usersWithEmail / totalUsers;
    return new SystemInsightResponse(
        totalUsers,
        usersWithEmail,
        adminUsers,
        totalUploads,
        todayUploads,
        totalStorageBytes,
        totalStorageGigabytes,
        emailCompletionRate
    );
  }

  private SystemConfig loadConfig() {
    SystemConfig config = systemConfigRepository.findById(1L)
        .orElseGet(() -> systemConfigRepository.save(new SystemConfig()));
    if (!StringUtils.hasText(config.getAssetDomain())) {
      config.setAssetDomain(DEFAULT_ASSET_DOMAIN);
      return systemConfigRepository.save(config);
    }
    return config;
  }

  private SystemConfigResponse toResponse(SystemConfig config) {
    return new SystemConfigResponse(
        config.getMaxUploadBytes(),
        bytesToMegabytes(config.getMaxUploadBytes()),
        config.getDailyUploadCountLimit(),
        config.getUserStorageQuotaBytes(),
        bytesToGigabytes(config.getUserStorageQuotaBytes()),
        config.isRegistrationEnabled(),
        config.isGuestLikeEnabled(),
        config.getAssetDomain(),
        config.getCustomFooterHtml(),
        config.getUpdatedAt(),
        config.getUpdatedBy()
    );
  }

  private String normalizeDomain(String domain) {
    String normalized = StringUtils.hasText(domain) ? domain.trim() : DEFAULT_ASSET_DOMAIN;
    if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
      normalized = "https://" + normalized;
    }
    return trimTrailingSlash(normalized);
  }

  private String normalizeFooterHtml(String footerHtml) {
    if (!StringUtils.hasText(footerHtml)) {
      return null;
    }
    return footerHtml.trim();
  }

  private String trimTrailingSlash(String value) {
    String result = value;
    while (result.length() > 1 && result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  private double bytesToMegabytes(long bytes) {
    return Math.round((bytes / (double) BYTES_PER_MB) * 10d) / 10d;
  }

  private double bytesToGigabytes(long bytes) {
    return Math.round((bytes / (double) BYTES_PER_GB) * 10d) / 10d;
  }

  private Instant startOfToday() {
    return LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
  }

  private String resolveUsername(Authentication authentication) {
    if (authentication == null) {
      return "system";
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof UserDetails userDetails) {
      return userDetails.getUsername();
    }
    return Optional.ofNullable(authentication.getName()).orElse("system");
  }
}
