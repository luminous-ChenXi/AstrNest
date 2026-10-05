package com.chenxi.astrnest.system;

import com.chenxi.astrnest.chenxi.mail.ChenxiMailConfigService;
import com.chenxi.astrnest.common.HtmlSanitizer;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.system.dto.PublicSystemConfigResponse;
import com.chenxi.astrnest.system.dto.SystemConfigResponse;
import com.chenxi.astrnest.system.dto.SystemInsightResponse;
import com.chenxi.astrnest.system.dto.UpdateSystemConfigRequest;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
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
  /** 云密钥对外统一掩码：读取时返回该值，保存时收到该值或空白则保留数据库原值 */
  public static final String SECRET_MASK = "******";

  private final SystemConfigRepository systemConfigRepository;
  private final UserAccountRepository userAccountRepository;
  private final UploadRecordRepository uploadRecordRepository;
  private final Environment environment;
  private final ChenxiMailConfigService chenxiMailConfigService;

  public SystemConfigResponse getCurrentConfig() {
    return toResponse(loadConfig());
  }

  public PublicSystemConfigResponse getPublicConfig() {
    SystemConfig config = loadConfig();
    return new PublicSystemConfigResponse(
        config.getCustomFooterHtml(),
        config.getAutoCleanupDays(),
        config.getAssetDomain(),
        config.getMaxFilesPerUpload(),
        (int) bytesToMegabytes(config.getMaxUploadBytes()),
        (int) bytesToMegabytes(config.getMaxVideoUploadBytes()),
        config.isVideoChunkUploadEnabled(),
        config.getVideoChunkSizeMb(),
        config.isGuestUploadEnabled(),
        config.isRegistrationEnabled(),
        config.isRegistrationEmailVerifyRequired(),
        true
    );
  }

  @Transactional
  public SystemConfigResponse updateConfig(UpdateSystemConfigRequest request, Authentication authentication) {
    SystemConfig config = loadConfig();
    config.setMaxUploadBytes(request.maxUploadMb() * BYTES_PER_MB);
    config.setMaxVideoUploadBytes(request.maxVideoUploadMb() * BYTES_PER_MB);
    config.setVideoChunkUploadEnabled(Boolean.TRUE.equals(request.videoChunkUploadEnabled()));
    config.setAutoCleanupDays(request.autoCleanupDays());
    config.setVideoChunkSizeMb(request.videoChunkSizeMb());
    config.setDailyUploadCountLimit(request.dailyUploadCountLimit());
    config.setMaxFilesPerUpload(request.maxFilesPerUpload());
    config.setUserStorageQuotaBytes(request.userStorageQuotaGb() * BYTES_PER_GB);
    config.setRegistrationEnabled(Boolean.TRUE.equals(request.registrationEnabled()));
    if (request.emailVerifyRequired() != null) {
      config.setRegistrationEmailVerifyRequired(request.emailVerifyRequired());
    }
    if (request.totpRequired() != null) {
      config.setLoginTotpRequired(request.totpRequired());
    }
    config.setGuestLikeEnabled(request.guestLikeEnabled() == null ? config.isGuestLikeEnabled() : request.guestLikeEnabled());
    config.setGuestUploadEnabled(request.guestUploadEnabled() == null ? config.isGuestUploadEnabled() : request.guestUploadEnabled());
    config.setAutoCleanupDays(request.autoCleanupDays());
    config.setAssetDomain(normalizeDomain(request.assetDomain()));
    config.setCustomFooterHtml(normalizeFooterHtml(request.customFooterHtml()));

    if (request.aiModerationEnabled() != null) {
      config.setAiModerationEnabled(request.aiModerationEnabled());
    }
    if (request.aiLabelingEnabled() != null) {
      config.setAiLabelingEnabled(request.aiLabelingEnabled());
    }
    if (request.aiTencentSecretId() != null) {
      config.setAiTencentSecretId(resolveSecretOnSave(config.getAiTencentSecretId(), request.aiTencentSecretId()));
    }
    if (request.aiTencentSecretKey() != null) {
      config.setAiTencentSecretKey(resolveSecretOnSave(config.getAiTencentSecretKey(), request.aiTencentSecretKey()));
    }
    if (request.aiTencentRegion() != null) {
      config.setAiTencentRegion(trimToNull(request.aiTencentRegion()));
    }
    if (request.aiTencentBucket() != null) {
      config.setAiTencentBucket(trimToNull(request.aiTencentBucket()));
    }
    if (request.aiTencentDetectScenes() != null) {
      config.setAiTencentDetectScenes(trimToNull(request.aiTencentDetectScenes()));
    }
    if (request.aiModerationBlockConfidence() != null) {
      config.setAiModerationBlockConfidence(request.aiModerationBlockConfidence());
    }
    if (request.aiModerationReviewConfidence() != null) {
      config.setAiModerationReviewConfidence(request.aiModerationReviewConfidence());
    }
    if (request.aiLabelMinConfidence() != null) {
      config.setAiLabelMinConfidence(request.aiLabelMinConfidence());
    }

    config.setUpdatedBy(resolveUsername(authentication));
    SystemConfig saved = systemConfigRepository.save(config);
    return toResponse(saved);
  }

  public long currentMaxUploadBytes() {
    return loadConfig().getMaxUploadBytes();
  }

  public long currentMaxVideoUploadBytes() {
    return loadConfig().getMaxVideoUploadBytes();
  }

  public boolean isVideoChunkUploadEnabled() {
    return loadConfig().isVideoChunkUploadEnabled();
  }

  public int currentVideoChunkSizeMb() {
    return loadConfig().getVideoChunkSizeMb();
  }

  public String currentAssetDomain() {
    return loadConfig().getAssetDomain();
  }

  public boolean isRegistrationEnabled() {
    return loadConfig().isRegistrationEnabled();
  }

  /** 注册是否必须邮箱验证（registration.email_verify_required）。 */
  public boolean isEmailVerifyRequired() {
    return loadConfig().isRegistrationEmailVerifyRequired();
  }

  /** 登录是否强制 TOTP 二步验证（login.totp_required）。 */
  public boolean isTotpRequired() {
    return loadConfig().isLoginTotpRequired();
  }

  /** 管理端「注册与登录安全」页保存：仅更新两个站长开关（null 表示不修改）。 */
  @Transactional
  public void updateSecuritySwitches(Boolean emailVerifyRequired, Boolean totpRequired) {
    SystemConfig config = loadConfig();
    if (emailVerifyRequired != null) {
      config.setRegistrationEmailVerifyRequired(emailVerifyRequired);
    }
    if (totpRequired != null) {
      config.setLoginTotpRequired(totpRequired);
    }
    config.setUpdatedBy("admin-security-settings");
    systemConfigRepository.save(config);
  }

  /**
   * SMTP 是否已配置并启用（决定邮箱验证等邮件能力是否可用）。
   *
   * <p>host + from 均非空即视为已配置，兼容无鉴权 SMTP（MailPit/内网中继）场景：
   * 密码仅在同时配置了 smtpUsername 时才必填，CHANGE_ME 占位符不视为真实密码。</p>
   */
  public boolean isSmtpConfigured() {
    try {
      var mailConfig = chenxiMailConfigService.getOrDefault();
      if (!mailConfig.isEnabled()
          || !StringUtils.hasText(mailConfig.getSmtpHost())
          || !StringUtils.hasText(mailConfig.getFromEmail())
          || "smtp.example.com".equals(mailConfig.getSmtpHost())) {
        return false;
      }
      boolean usernameConfigured = StringUtils.hasText(mailConfig.getSmtpUsername());
      boolean passwordValid = StringUtils.hasText(mailConfig.getSmtpPassword())
          && !"CHANGE_ME".equals(mailConfig.getSmtpPassword());
      return !usernameConfigured || passwordValid;
    } catch (Exception exception) {
      return false;
    }
  }

  public boolean isGuestLikeEnabled() {
    return loadConfig().isGuestLikeEnabled();
  }

  public boolean isGuestUploadEnabled() {
    return loadConfig().isGuestUploadEnabled();
  }

  public int currentMaxFilesPerUpload() {
    return loadConfig().getMaxFilesPerUpload();
  }

  public int currentGuestDailyUploadLimit() {
    // 访客每日上传限制：默认为登录用户限制的 20%，最少 5 个，最多 20 个
    int userLimit = loadConfig().getMaxFilesPerUpload();
    int guestLimit = Math.max(5, Math.min(20, userLimit / 5));
    return guestLimit;
  }

  public int currentAutoCleanupDays() {
    return loadConfig().getAutoCleanupDays();
  }

  public TencentAiSettings currentTencentAiSettings() {
    SystemConfig config = loadConfig();
    return new TencentAiSettings(
        config.isAiModerationEnabled(),
        config.isAiLabelingEnabled(),
        configOrEnv(config.getAiTencentSecretId(), "ASTRNEST_AI_TENCENT_SECRET_ID"),
        configOrEnv(config.getAiTencentSecretKey(), "ASTRNEST_AI_TENCENT_SECRET_KEY"),
        configOrEnv(config.getAiTencentRegion(), "ASTRNEST_AI_TENCENT_REGION"),
        configOrEnv(config.getAiTencentBucket(), "ASTRNEST_AI_TENCENT_BUCKET"),
        configOrEnv(config.getAiTencentDetectScenes(), "ASTRNEST_AI_TENCENT_DETECT_SCENES"),
        configOrEnv(config.getAiModerationBlockConfidence(), "ASTRNEST_AI_BLOCK_CONFIDENCE", 90),
        configOrEnv(config.getAiModerationReviewConfidence(), "ASTRNEST_AI_REVIEW_CONFIDENCE", 60),
        configOrEnv(config.getAiLabelMinConfidence(), "ASTRNEST_AI_LABEL_MIN_CONFIDENCE", 60)
    );
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
    return systemConfigRepository.findById(1L)
        .orElseGet(() -> systemConfigRepository.save(new SystemConfig()));
  }

  private SystemConfigResponse toResponse(SystemConfig config) {
    return new SystemConfigResponse(
        config.getMaxUploadBytes(),
        bytesToMegabytes(config.getMaxUploadBytes()),
        config.getMaxVideoUploadBytes(),
        bytesToMegabytes(config.getMaxVideoUploadBytes()),
        config.getDailyUploadCountLimit(),
        config.getMaxFilesPerUpload(),
        config.getUserStorageQuotaBytes(),
        bytesToGigabytes(config.getUserStorageQuotaBytes()),
        config.isRegistrationEnabled(),
        config.isGuestLikeEnabled(),
        config.isGuestUploadEnabled(),
        config.getAutoCleanupDays(),
        config.isVideoChunkUploadEnabled(),
        config.getVideoChunkSizeMb(),
        config.getAssetDomain(),
        config.getCustomFooterHtml(),
        config.isAiModerationEnabled(),
        config.isAiLabelingEnabled(),
        maskSecret(config.getAiTencentSecretId()),
        maskSecret(config.getAiTencentSecretKey()),
        config.getAiTencentRegion(),
        config.getAiTencentBucket(),
        config.getAiTencentDetectScenes(),
        defaultInt(config.getAiModerationBlockConfidence(), 90),
        defaultInt(config.getAiModerationReviewConfidence(), 60),
        defaultInt(config.getAiLabelMinConfidence(), 60),
        config.isRegistrationEmailVerifyRequired(),
        config.isLoginTotpRequired(),
        isSmtpConfigured(),
        config.getUpdatedAt(),
        config.getUpdatedBy()
    );
  }

  private String normalizeDomain(String domain) {
    if (!StringUtils.hasText(domain)) {
      return null;
    }
    String normalized = domain.trim();
    if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
      normalized = "https://" + normalized;
    }
    return trimTrailingSlash(normalized);
  }

  private String normalizeFooterHtml(String footerHtml) {
    if (!StringUtils.hasText(footerHtml)) {
      return null;
    }
    // 白名单清洗（审计 P1-11）：footer 是注入全站的唯一 HTML 通道，服务端不清洗则防护全押前端 DOMPurify
    return HtmlSanitizer.richText(footerHtml);
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /** 读取时掩码：非空密钥统一返回固定掩码值，避免明文回显 */
  private String maskSecret(String secret) {
    return StringUtils.hasText(secret) ? SECRET_MASK : null;
  }

  /** 保存时解析密钥：提交掩码或空白视为「不修改」，保留数据库原值 */
  private String resolveSecretOnSave(String currentValue, String incoming) {
    if (!StringUtils.hasText(incoming) || SECRET_MASK.equals(incoming.trim())) {
      return currentValue;
    }
    return trimToNull(incoming);
  }

  private int defaultInt(Integer value, int fallback) {
    return value == null ? fallback : value;
  }

  private String configOrEnv(String persisted, String envKey) {
    if (StringUtils.hasText(persisted)) {
      return persisted;
    }
    return trimToNull(envOrNull(envKey));
  }

  private int configOrEnv(Integer persisted, String envKey, int fallback) {
    if (persisted != null) {
      return persisted;
    }
    String envValue = envOrNull(envKey);
    if (envValue == null) {
      return fallback;
    }
    String trimmed = envValue.trim();
    if (trimmed.isEmpty()) {
      return fallback;
    }
    try {
      return Integer.parseInt(trimmed);
    } catch (NumberFormatException exception) {
      return fallback;
    }
  }

  @Nullable
  @SuppressWarnings("null")
  private String envOrNull(String key) {
    return environment.getProperty(key);
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

  public record TencentAiSettings(
      boolean moderationEnabled,
      boolean labelingEnabled,
      String secretId,
      String secretKey,
      String region,
      String bucket,
      String detectScenes,
      int moderationBlockConfidence,
      int moderationReviewConfidence,
      int labelMinConfidence
  ) {

    public boolean hasCredentials() {
      return StringUtils.hasText(secretId)
          && StringUtils.hasText(secretKey)
          && StringUtils.hasText(region)
          && StringUtils.hasText(bucket);
    }

    public boolean moderationReady() {
      return moderationEnabled && hasCredentials();
    }

    public boolean labelingReady() {
      return labelingEnabled && hasCredentials();
    }

    public String[] detectScenesArray() {
      if (!StringUtils.hasText(detectScenes)) {
        return new String[0];
      }
      return Arrays.stream(detectScenes.split(","))
          .map(String::trim)
          .filter(StringUtils::hasText)
          .toArray(String[]::new);
    }
  }
}
