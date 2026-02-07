package com.imgbed.security.apikey;

import com.imgbed.security.apikey.dto.ApiKeyResponse;
import com.imgbed.security.apikey.dto.CreateApiKeyRequest;
import com.imgbed.security.apikey.dto.CreateApiKeyResponse;
import com.imgbed.security.apikey.dto.UpdateApiKeyQuotaRequest;
import com.imgbed.security.apikey.dto.UpdateApiKeyStatusRequest;
import com.imgbed.security.apikey.exception.ApiKeyAuthenticationException;
import com.imgbed.security.apikey.exception.ApiKeyQuotaExceededException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final char[] SECRET_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789".toCharArray();
  private static final Pattern KEY_FORMAT = Pattern.compile("^ik_([a-zA-Z0-9]{32})_([A-Za-z0-9]{40})$");

  private final ApiKeyRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final ApiKeyProperties properties;

  @Transactional
  public CreateApiKeyResponse createKey(CreateApiKeyRequest request) {
    ApiKey key = new ApiKey();
    key.setName(request.name());
    key.setDescription(request.description());
    key.setDailyQuota(Optional.ofNullable(request.dailyQuota()).orElse(properties.getDefaultDailyQuota()));

    String publicId = generateUniquePublicId();
    String secret = generateSecret();
    key.setPublicId(publicId);
    key.setSecretHash(passwordEncoder.encode(secret));
    key.setMaskedKey(maskPublicId(publicId));

    ApiKey saved = repository.save(key);
    return new CreateApiKeyResponse(toResponse(saved), buildPlainValue(publicId, secret));
  }

  @Transactional(readOnly = true)
  public List<ApiKeyResponse> listKeys() {
    return repository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional
  public ApiKeyResponse updateStatus(Long id, UpdateApiKeyStatusRequest request) {
    ApiKey key = getKeyOrThrow(id);
    key.setActive(request.active());
    return toResponse(key);
  }

  @Transactional
  public ApiKeyResponse updateQuota(Long id, UpdateApiKeyQuotaRequest request) {
    ApiKey key = getKeyOrThrow(id);
    key.setDailyQuota(request.dailyQuota());
    if (key.getRequestsToday() > request.dailyQuota()) {
      key.setRequestsToday(request.dailyQuota());
    }
    return toResponse(key);
  }

  @Transactional
  public void delete(Long id) {
    repository.deleteById(id);
  }

  @Transactional
  public CreateApiKeyResponse resetSecret(Long id) {
    ApiKey key = getKeyOrThrow(id);
    String secret = generateSecret();
    key.setSecretHash(passwordEncoder.encode(secret));
    key.setMaskedKey(maskPublicId(key.getPublicId()));
    key.setRequestsToday(0);
    key.setRequestCount(0);
    key.setLastRequestDate(null);
    key.setLastUsedAt(null);
    ApiKey saved = repository.save(key);
    return new CreateApiKeyResponse(toResponse(saved), buildPlainValue(saved.getPublicId(), secret));
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

  private void enforceQuota(ApiKey apiKey) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    if (apiKey.getLastRequestDate() == null || !apiKey.getLastRequestDate().equals(today)) {
      apiKey.setLastRequestDate(today);
      apiKey.setRequestsToday(0);
    }
    if (apiKey.getRequestsToday() >= apiKey.getDailyQuota()) {
      throw new ApiKeyQuotaExceededException("API Key 今日调用次数已达上限");
    }
    apiKey.setRequestsToday(apiKey.getRequestsToday() + 1);
    apiKey.setRequestCount(apiKey.getRequestCount() + 1);
    apiKey.setLastUsedAt(Instant.now());
    repository.save(apiKey);
  }

  private ApiKey getKeyOrThrow(Long id) {
    return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("API Key 不存在"));
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

  private ApiKeyResponse toResponse(ApiKey apiKey) {
    return new ApiKeyResponse(
        apiKey.getId(),
        apiKey.getName(),
        apiKey.getDescription(),
        apiKey.getMaskedKey(),
        apiKey.isActive(),
        apiKey.getRequestCount(),
        apiKey.getRequestsToday(),
        apiKey.getDailyQuota(),
        apiKey.getCreatedAt(),
        apiKey.getLastUsedAt()
    );
  }
}
