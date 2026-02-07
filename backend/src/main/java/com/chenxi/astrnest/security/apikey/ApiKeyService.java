package com.chenxi.astrnest.security.apikey;

import com.chenxi.astrnest.security.apikey.dto.ApiKeyDashboardResponse;
import com.chenxi.astrnest.security.apikey.dto.ApiKeyOwnerInfo;
import com.chenxi.astrnest.security.apikey.dto.ApiKeyOwnerSummary;
import com.chenxi.astrnest.security.apikey.dto.ApiKeyResponse;
import com.chenxi.astrnest.security.apikey.dto.ApiKeyUsageAggregate;
import com.chenxi.astrnest.security.apikey.dto.CreateApiKeyRequest;
import com.chenxi.astrnest.security.apikey.dto.CreateApiKeyResponse;
import com.chenxi.astrnest.security.apikey.dto.UpdateApiKeyQuotaRequest;
import com.chenxi.astrnest.security.apikey.dto.UpdateApiKeyStatusRequest;
import com.chenxi.astrnest.security.apikey.exception.ApiKeyAuthenticationException;
import com.chenxi.astrnest.security.apikey.exception.ApiKeyQuotaExceededException;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import com.chenxi.astrnest.upload.record.dto.UserUsageAggregate;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final char[] SECRET_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789".toCharArray();
  private static final Pattern KEY_FORMAT = Pattern.compile("^ik_([a-zA-Z0-9]{32})_([A-Za-z0-9]{40})$");

  private final ApiKeyRepository repository;
  private final UploadRecordRepository uploadRecordRepository;
  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder;
  private final ApiKeyProperties properties;

  @Transactional
  public CreateApiKeyResponse createKey(CreateApiKeyRequest request) {
    return createKeyInternal(request, request.ownerId());
  }

  @Transactional
  public CreateApiKeyResponse createKeyForOwner(CreateApiKeyRequest request, Long ownerId) {
    return createKeyInternal(request, ownerId);
  }

  @Transactional(readOnly = true)
  public List<ApiKeyResponse> listKeys(String search, Long ownerId, Boolean activeOnly) {
    List<ApiKey> keys = repository.findAll();
    Stream<ApiKey> stream = keys.stream();
    if (StringUtils.hasText(search)) {
      String keyword = search.trim().toLowerCase(Locale.ROOT);
      stream = stream.filter(key -> matchesSearch(key, keyword));
    }
    if (ownerId != null) {
      stream = stream.filter(key -> key.getOwner() != null && ownerId.equals(key.getOwner().getId()));
    }
    if (Boolean.TRUE.equals(activeOnly)) {
      stream = stream.filter(ApiKey::isActive);
    }
    List<ApiKey> filtered = stream
        .sorted(Comparator.comparing(ApiKey::getCreatedAt).reversed())
        .toList();
    Map<Long, ApiKeyUsageAggregate> usageMap = usageSnapshot(filtered);
    return filtered.stream()
        .map(key -> toResponse(key, usageMap.get(key.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ApiKeyResponse> listKeysForOwner(Long ownerId) {
    List<ApiKey> keys = repository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    Map<Long, ApiKeyUsageAggregate> usageMap = usageSnapshot(keys);
    return keys.stream()
        .map(key -> toResponse(key, usageMap.get(key.getId())))
        .toList();
  }

  @Transactional
  public ApiKeyResponse updateStatus(Long id, UpdateApiKeyStatusRequest request) {
    return updateStatusInternal(id, request, null);
  }

  @Transactional
  public ApiKeyResponse updateStatusForOwner(Long id, UpdateApiKeyStatusRequest request, Long ownerId) {
    return updateStatusInternal(id, request, ownerId);
  }

  @Transactional
  public ApiKeyResponse updateQuota(Long id, UpdateApiKeyQuotaRequest request) {
    return updateQuotaInternal(id, request, null);
  }

  @Transactional
  public ApiKeyResponse updateQuotaForOwner(Long id, UpdateApiKeyQuotaRequest request, Long ownerId) {
    return updateQuotaInternal(id, request, ownerId);
  }

  @Transactional
  public void delete(Long id) {
    Long keyId = Objects.requireNonNull(id, "API Key id 不能为空");
    repository.deleteById(keyId);
  }

  @Transactional
  public void deleteOwned(Long id, Long ownerId) {
    ApiKey key = Objects.requireNonNull(getKeyOrThrow(id));
    ensureOwnership(key, ownerId);
    repository.delete(key);
  }

  @Transactional
  public CreateApiKeyResponse resetSecret(Long id) {
    return resetSecretInternal(id, null);
  }

  @Transactional
  public CreateApiKeyResponse resetSecretForOwner(Long id, Long ownerId) {
    return resetSecretInternal(id, ownerId);
  }

  @Transactional(readOnly = true)
  public ApiKeyDashboardResponse dashboard() {
    List<ApiKey> keys = repository.findAll();
    long totalRequests = keys.stream().mapToLong(ApiKey::getRequestCount).sum();
    long todaysRequests = keys.stream().mapToLong(ApiKey::getRequestsToday).sum();
    long activeKeys = keys.stream().filter(ApiKey::isActive).count();
    long totalOwners = keys.stream()
        .map(ApiKey::getOwner)
        .filter(Objects::nonNull)
        .map(UserAccount::getId)
        .distinct()
        .count();
    Instant startOfDay = startOfTodayUtc();
    long totalUploadsViaApi = uploadRecordRepository.countApiUploads();
    long todayUploadsViaApi = uploadRecordRepository.countApiUploadsAfter(startOfDay);
    return new ApiKeyDashboardResponse(
        keys.size(),
        activeKeys,
        totalRequests,
        todaysRequests,
        totalOwners,
        totalUploadsViaApi,
        todayUploadsViaApi
    );
  }

  @Transactional(readOnly = true)
  public List<ApiKeyOwnerSummary> ownerSummaries() {
    List<ApiKey> keys = repository.findAll();
    if (keys.isEmpty()) {
      return List.of();
    }
    Map<Long, OwnerAccumulator> accumulatorMap = new LinkedHashMap<>();
    for (ApiKey key : keys) {
      OwnerAccumulator accumulator = accumulatorMap.computeIfAbsent(ownerKey(key), id -> OwnerAccumulator.from(key.getOwner()));
      accumulator.increment(key);
    }
    List<Long> ownerIds = accumulatorMap.keySet().stream().filter(Objects::nonNull).toList();
    Map<Long, UserUsageAggregate> usageMap = usageByOwner(ownerIds);
    return accumulatorMap.values().stream()
        .sorted(Comparator.comparingLong(OwnerAccumulator::getKeyCount).reversed())
        .map(acc -> toOwnerSummary(acc, usageMap.get(acc.getOwnerId())))
        .toList();
  }

  @Transactional
  public ApiKey authenticate(String rawKey) {
    if (!StringUtils.hasText(rawKey)) {
      throw new ApiKeyAuthenticationException("未提供 API Key");
    }
    Matcher matcher = KEY_FORMAT.matcher(rawKey.trim());
    if (!matcher.matches()) {
      throw new ApiKeyAuthenticationException("API Key 格式不正确");
    }
    String publicId = matcher.group(1);
    String secret = matcher.group(2);
    ApiKey apiKey = repository.findByPublicId(publicId)
        .orElseThrow(() -> new ApiKeyAuthenticationException("API Key 无效"));
    if (!apiKey.isActive()) {
      throw new ApiKeyAuthenticationException("API Key 已被禁用");
    }
    if (!passwordEncoder.matches(secret, apiKey.getSecretHash())) {
      throw new ApiKeyAuthenticationException("API Key 不匹配");
    }
    enforceQuota(apiKey);
    return apiKey;
  }

  private CreateApiKeyResponse createKeyInternal(CreateApiKeyRequest request, Long ownerIdOverride) {
    ApiKey key = new ApiKey();
    key.setName(request.name());
    key.setDescription(request.description());
    key.setDailyQuota(Optional.ofNullable(request.dailyQuota()).orElse(properties.getDefaultDailyQuota()));
    key.setPerMinuteQuota(Optional.ofNullable(request.perMinuteQuota()).orElse(properties.getDefaultPerMinuteQuota()));

    if (ownerIdOverride != null) {
      UserAccount owner = userAccountRepository.findById(ownerIdOverride)
          .orElseThrow(() -> new IllegalArgumentException("未找到指定的用户"));
      key.setOwner(owner);
    }

    String publicId = generateUniquePublicId();
    String secret = generateSecret();
    key.setPublicId(publicId);
    key.setSecretHash(passwordEncoder.encode(secret));
    key.setMaskedKey(maskPublicId(publicId));

    ApiKey saved = repository.save(key);
    return new CreateApiKeyResponse(toResponse(saved, null), buildPlainValue(publicId, secret));
  }

  private ApiKeyResponse updateStatusInternal(Long id, UpdateApiKeyStatusRequest request, Long ownerGuard) {
    ApiKey key = getKeyOrThrow(id);
    ensureOwnership(key, ownerGuard);
    key.setActive(request.active());
    ApiKey saved = repository.save(key);
    return toResponse(saved, usageAggregateFor(saved));
  }

  private ApiKeyResponse updateQuotaInternal(Long id, UpdateApiKeyQuotaRequest request, Long ownerGuard) {
    ApiKey key = getKeyOrThrow(id);
    ensureOwnership(key, ownerGuard);
    key.setDailyQuota(request.dailyQuota());
    if (key.getRequestsToday() > request.dailyQuota()) {
      key.setRequestsToday(request.dailyQuota());
    }
    key.setPerMinuteQuota(request.perMinuteQuota());
    if (key.getRequestsCurrentMinute() > request.perMinuteQuota()) {
      key.setRequestsCurrentMinute(request.perMinuteQuota());
    }
    ApiKey saved = repository.save(key);
    return toResponse(saved, usageAggregateFor(saved));
  }

  private CreateApiKeyResponse resetSecretInternal(Long id, Long ownerGuard) {
    ApiKey key = getKeyOrThrow(id);
    ensureOwnership(key, ownerGuard);
    String secret = generateSecret();
    key.setSecretHash(passwordEncoder.encode(secret));
    key.setMaskedKey(maskPublicId(key.getPublicId()));
    key.setRequestsToday(0);
    key.setRequestCount(0);
    key.setLastRequestDate(null);
    key.setLastUsedAt(null);
    key.setRequestsCurrentMinute(0);
    key.setCurrentMinuteWindow(null);
    ApiKey saved = repository.save(key);
    return new CreateApiKeyResponse(toResponse(saved, null), buildPlainValue(saved.getPublicId(), secret));
  }

  private void enforceQuota(ApiKey apiKey) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    if (apiKey.getLastRequestDate() == null || !apiKey.getLastRequestDate().equals(today)) {
      apiKey.setLastRequestDate(today);
      apiKey.setRequestsToday(0);
    }
    if (apiKey.getRequestsToday() >= apiKey.getDailyQuota()) {
      throw new ApiKeyQuotaExceededException("API Key 今日调用次数已达上限");
    }
    enforceMinuteQuota(apiKey);
    apiKey.setRequestsToday(apiKey.getRequestsToday() + 1);
    apiKey.setRequestCount(apiKey.getRequestCount() + 1);
    apiKey.setLastUsedAt(Instant.now());
    repository.save(apiKey);
  }

  private void enforceMinuteQuota(ApiKey apiKey) {
    Instant currentMinute = Instant.now().truncatedTo(ChronoUnit.MINUTES);
    if (apiKey.getCurrentMinuteWindow() == null || apiKey.getCurrentMinuteWindow().isBefore(currentMinute)) {
      apiKey.setCurrentMinuteWindow(currentMinute);
      apiKey.setRequestsCurrentMinute(0);
    }
    if (apiKey.getRequestsCurrentMinute() >= apiKey.getPerMinuteQuota()) {
      throw new ApiKeyQuotaExceededException("API Key 已达到分钟级限流阈值");
    }
    apiKey.setRequestsCurrentMinute(apiKey.getRequestsCurrentMinute() + 1);
  }

  private ApiKey getKeyOrThrow(Long id) {
    Long keyId = Objects.requireNonNull(id, "API Key id 不能为空");
    return repository.findById(keyId).orElseThrow(() -> new IllegalArgumentException("API Key 不存在"));
  }

  private ApiKeyResponse toResponse(ApiKey apiKey, ApiKeyUsageAggregate usage) {
    long uploadCount = usage != null ? usage.uploadCount() : 0;
    long todayUploadCount = usage != null ? usage.todayUploadCount() : 0;
    Instant lastUploadAt = usage != null ? usage.lastUploadAt() : null;
    return new ApiKeyResponse(
        apiKey.getId(),
        apiKey.getName(),
        apiKey.getDescription(),
        apiKey.getMaskedKey(),
        apiKey.isActive(),
        apiKey.getRequestCount(),
        apiKey.getRequestsToday(),
        apiKey.getRequestsCurrentMinute(),
        apiKey.getDailyQuota(),
        apiKey.getPerMinuteQuota(),
        uploadCount,
        todayUploadCount,
        lastUploadAt,
        apiKey.getCreatedAt(),
        apiKey.getLastUsedAt(),
        buildOwnerInfo(apiKey)
    );
  }

  private ApiKeyOwnerSummary toOwnerSummary(OwnerAccumulator acc, UserUsageAggregate usage) {
    long uploadCount = usage != null ? usage.uploadCount() : 0;
    long storageBytes = usage != null ? usage.storageBytes() : 0;
    return new ApiKeyOwnerSummary(
        acc.getOwnerId(),
        acc.getUsername(),
        acc.getDisplayName(),
        acc.getKeyCount(),
        acc.getActiveKeyCount(),
        acc.getTotalRequests(),
        acc.getTodaysRequests(),
        uploadCount,
        storageBytes
    );
  }

  private ApiKeyOwnerInfo buildOwnerInfo(ApiKey apiKey) {
    if (apiKey.getOwner() == null) {
      return new ApiKeyOwnerInfo(null, "system", "系统接口");
    }
    return new ApiKeyOwnerInfo(apiKey.getOwner().getId(), apiKey.getOwner().getUsername(), apiKey.getOwner().getDisplayName());
  }

  private Map<Long, ApiKeyUsageAggregate> usageSnapshot(List<ApiKey> keys) {
    if (CollectionUtils.isEmpty(keys)) {
      return Map.of();
    }
    List<Long> keyIds = keys.stream().map(ApiKey::getId).toList();
    Instant startOfDay = startOfTodayUtc();
    return uploadRecordRepository.aggregateApiUsageByKeyIds(keyIds, startOfDay).stream()
        .collect(Collectors.toMap(ApiKeyUsageAggregate::apiKeyId, aggregate -> aggregate));
  }

  private ApiKeyUsageAggregate usageAggregateFor(ApiKey key) {
    Map<Long, ApiKeyUsageAggregate> snapshot = usageSnapshot(List.of(key));
    return snapshot.get(key.getId());
  }

  private Map<Long, UserUsageAggregate> usageByOwner(List<Long> ownerIds) {
    if (CollectionUtils.isEmpty(ownerIds)) {
      return Map.of();
    }
    return uploadRecordRepository.aggregateUsageByUserIds(ownerIds).stream()
        .collect(Collectors.toMap(UserUsageAggregate::userId, aggregate -> aggregate));
  }

  private boolean matchesSearch(ApiKey key, String keyword) {
    if (key.getName() != null && key.getName().toLowerCase(Locale.ROOT).contains(keyword)) {
      return true;
    }
    if (key.getDescription() != null && key.getDescription().toLowerCase(Locale.ROOT).contains(keyword)) {
      return true;
    }
    if (key.getMaskedKey() != null && key.getMaskedKey().contains(keyword)) {
      return true;
    }
    if (key.getOwner() != null) {
      String username = key.getOwner().getUsername();
      String displayName = key.getOwner().getDisplayName();
      boolean usernameMatch = username != null && username.toLowerCase(Locale.ROOT).contains(keyword);
      boolean displayMatch = displayName != null && displayName.toLowerCase(Locale.ROOT).contains(keyword);
      return usernameMatch || displayMatch;
    }
    return false;
  }

  private String buildPlainValue(String publicId, String secret) {
    return "ik_%s_%s".formatted(publicId, secret);
  }

  private String maskPublicId(String publicId) {
    String start = publicId.substring(0, 6);
    String end = publicId.substring(publicId.length() - 4);
    return "%s...%s".formatted(start, end).toLowerCase(Locale.ROOT);
  }

  private String generateUniquePublicId() {
    String candidate;
    do {
      candidate = randomAlphaNumeric(32).toLowerCase(Locale.ROOT);
    } while (repository.findByPublicId(candidate).isPresent());
    return candidate;
  }

  private String generateSecret() {
    return randomAlphaNumeric(40);
  }

  private String randomAlphaNumeric(int length) {
    char[] buffer = new char[length];
    for (int i = 0; i < length; i++) {
      buffer[i] = SECRET_ALPHABET[RANDOM.nextInt(SECRET_ALPHABET.length)];
    }
    return new String(buffer);
  }

  private void ensureOwnership(ApiKey key, Long ownerGuard) {
    if (ownerGuard == null) {
      return;
    }
    Long keyOwnerId = key.getOwner() != null ? key.getOwner().getId() : null;
    if (!ownerGuard.equals(keyOwnerId)) {
      throw new AccessDeniedException("无权操作该 API Key");
    }
  }

  private Instant startOfTodayUtc() {
    return LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
  }

  private Long ownerKey(ApiKey key) {
    return key.getOwner() != null ? key.getOwner().getId() : null;
  }

  private static class OwnerAccumulator {

    private final Long ownerId;
    private final String username;
    private final String displayName;
    private long keyCount;
    private long activeKeyCount;
    private long totalRequests;
    private long todaysRequests;

    private OwnerAccumulator(Long ownerId, String username, String displayName) {
      this.ownerId = ownerId;
      this.username = username;
      this.displayName = displayName;
    }

    private void increment(ApiKey key) {
      keyCount++;
      if (key.isActive()) {
        activeKeyCount++;
      }
      totalRequests += key.getRequestCount();
      todaysRequests += key.getRequestsToday();
    }

    private static OwnerAccumulator from(UserAccount owner) {
      if (owner == null) {
        return new OwnerAccumulator(null, "system", "系统接口");
      }
      return new OwnerAccumulator(owner.getId(), owner.getUsername(), owner.getDisplayName());
    }

    Long getOwnerId() {
      return ownerId;
    }

    String getUsername() {
      return username;
    }

    String getDisplayName() {
      return displayName;
    }

    long getKeyCount() {
      return keyCount;
    }

    long getActiveKeyCount() {
      return activeKeyCount;
    }

    long getTotalRequests() {
      return totalRequests;
    }

    long getTodaysRequests() {
      return todaysRequests;
    }
  }
}
