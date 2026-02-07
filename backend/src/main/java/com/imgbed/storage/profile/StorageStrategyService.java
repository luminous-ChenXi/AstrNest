package com.imgbed.storage.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imgbed.storage.StorageProperties;
import com.imgbed.storage.profile.dto.CreateStorageStrategyRequest;
import com.imgbed.storage.profile.dto.StorageStrategyProfileResponse;
import com.imgbed.storage.profile.dto.UpdateStorageStrategyRequest;
import jakarta.transaction.Transactional;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StorageStrategyService {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final StorageStrategyProfileRepository repository;
  private final StorageProperties storageProperties;
  private final ObjectMapper objectMapper;

  public List<StorageStrategyProfileResponse> listProfiles() {
    return repository.findAllByOrderByActiveDescUpdatedAtDesc().stream()
        .map(this::toResponse)
        .toList();
  }

  public StorageStrategyProfileResponse create(CreateStorageStrategyRequest request, Authentication authentication) {
    if (repository.existsByName(request.name())) {
      throw new IllegalArgumentException("策略名称已存在");
    }
    StorageStrategyProfile profile = new StorageStrategyProfile();
    profile.setStrategy(request.strategy());
    profile.setName(request.name());
    profile.setDisplayName(request.displayName());
    profile.setDescription(request.description());
    profile.setConfigJson(writeConfig(request.config()));
    profile.setCreatedBy(resolveOperator(authentication));
    profile.setUpdatedBy(resolveOperator(authentication));
    profile.setEnabled(true);
    if (Boolean.TRUE.equals(request.active())) {
      deactivateOthers(null);
      profile.setActive(true);
    }
    StorageStrategyProfile saved = repository.save(profile);
    if (saved.isActive()) {
      applyProfile(saved);
    }
    return toResponse(saved);
  }

  public StorageStrategyProfileResponse update(Long id, UpdateStorageStrategyRequest request, Authentication authentication) {
    StorageStrategyProfile profile = require(id);
    profile.setDisplayName(request.displayName());
    profile.setDescription(request.description());
    if (request.strategy() != null) {
      profile.setStrategy(request.strategy());
    }
    if (request.config() != null && !request.config().isEmpty()) {
      profile.setConfigJson(writeConfig(request.config()));
    }
    profile.setUpdatedBy(resolveOperator(authentication));
    if (request.active() != null) {
      if (request.active()) {
        deactivateOthers(profile.getId());
      }
      profile.setActive(request.active());
    }
    StorageStrategyProfile saved = repository.save(profile);
    if (saved.isActive()) {
      applyProfile(saved);
    }
    return toResponse(saved);
  }

  public void delete(Long id) {
    StorageStrategyProfile profile = require(id);
    if (profile.isActive()) {
      throw new IllegalStateException("请先切换到其他策略后再删除");
    }
    repository.delete(profile);
  }

  public StorageStrategyProfileResponse activate(Long id, Authentication authentication) {
    StorageStrategyProfile profile = require(id);
    if (profile.isActive()) {
      return toResponse(profile);
    }
    deactivateOthers(profile.getId());
    profile.setActive(true);
    profile.setUpdatedBy(resolveOperator(authentication));
    StorageStrategyProfile saved = repository.save(profile);
    applyProfile(saved);
    return toResponse(saved);
  }

  public void applyProfile(StorageStrategyProfile profile) {
    if (profile == null) {
      return;
    }
    Map<String, Object> config = readConfig(profile.getConfigJson());
    resetProviders();
    switch (profile.getStrategy()) {
      case LOCAL -> applyLocal(config);
      case ALIYUN_OSS -> applyAliyun(config);
      case TENCENT_COS -> applyS3(config, storageProperties.getCos());
      case QINIU_KODO -> applyS3(config, storageProperties.getKodo());
      case HUAWEI_OBS -> applyS3(config, storageProperties.getObs());
      case KS3 -> applyS3(config, storageProperties.getKs3());
      case S3_COMPATIBLE -> applyS3(config, storageProperties.getS3());
      case UPYUN_USS -> applyUpyun(config);
      case ONEDRIVE -> applyOnedrive(config);
      default -> throw new IllegalArgumentException("暂不支持的存储策略: " + profile.getStrategy());
    }
    storageProperties.setStrategy(profile.getStrategy());
    log.info("已应用存储策略 {} ({})", profile.getDisplayName(), profile.getStrategy());
  }

  public void applyActiveProfileOnStartup() {
    repository.findFirstByActiveTrue().ifPresentOrElse(this::applyProfile, () -> {
      log.info("未检测到激活的存储策略，沿用配置文件默认值: {}", storageProperties.getStrategy());
    });
  }

  private void deactivateOthers(Long keepId) {
    List<StorageStrategyProfile> toUpdate = new ArrayList<>();
    for (StorageStrategyProfile profile : repository.findAll()) {
      if (profile.isActive() && !Objects.equals(profile.getId(), keepId)) {
        profile.setActive(false);
        toUpdate.add(profile);
      }
    }
    if (!toUpdate.isEmpty()) {
      repository.saveAll(toUpdate);
    }
  }

  private StorageStrategyProfile require(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("策略 ID 不能为空");
    }
    return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("存储策略不存在"));
  }

  private StorageStrategyProfileResponse toResponse(StorageStrategyProfile profile) {
    return new StorageStrategyProfileResponse(
        profile.getId(),
        profile.getStrategy(),
        profile.getName(),
        profile.getDisplayName(),
        profile.getDescription(),
        profile.isActive(),
        profile.isEnabled(),
        readConfig(profile.getConfigJson()),
        profile.getUpdatedBy(),
        profile.getCreatedAt(),
        profile.getUpdatedAt()
    );
  }

  private String resolveOperator(Authentication authentication) {
    if (authentication == null) {
      return "system";
    }
    return authentication.getName();
  }

  private String writeConfig(Map<String, Object> config) {
    try {
      return objectMapper.writeValueAsString(config);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("配置序列化失败", exception);
    }
  }

  private Map<String, Object> readConfig(String json) {
    if (!StringUtils.hasText(json)) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(json, MAP_TYPE);
    } catch (JsonProcessingException exception) {
      log.warn("解析存储策略配置失败: {}", exception.getMessage());
      return Map.of();
    }
  }

  private void resetProviders() {
    storageProperties.getOss().setEnabled(false);
    storageProperties.getCos().setEnabled(false);
    storageProperties.getKodo().setEnabled(false);
    storageProperties.getObs().setEnabled(false);
    storageProperties.getKs3().setEnabled(false);
    storageProperties.getS3().setEnabled(false);
    storageProperties.getUpyun().setEnabled(false);
    storageProperties.getOnedrive().setEnabled(false);
  }

  private void applyLocal(Map<String, Object> config) {
    storageProperties.getLocal().setPublicBaseUrl(string(config, "publicBaseUrl", storageProperties.getLocal().getPublicBaseUrl()));
    String root = string(config, "root", null);
    if (StringUtils.hasText(root)) {
      try {
        Path path = Paths.get(root).toAbsolutePath().normalize();
        storageProperties.getLocal().setRoot(path);
      } catch (Exception ex) {
        throw new IllegalArgumentException("本地存储目录无效: " + root, ex);
      }
    }
  }

  private void applyAliyun(Map<String, Object> config) {
    var oss = storageProperties.getOss();
    oss.setEnabled(true);
    oss.setBucket(string(config, "bucket", oss.getBucket()));
    oss.setEndpoint(string(config, "endpoint", oss.getEndpoint()));
    oss.setAccessKey(string(config, "accessKey", oss.getAccessKey()));
    oss.setSecretKey(string(config, "secretKey", oss.getSecretKey()));
    oss.setCdnHost(string(config, "cdnHost", oss.getCdnHost()));
    oss.setEnableCname(bool(config, "enableCname", oss.isEnableCname()));
    oss.setInternalEndpoint(bool(config, "internalEndpoint", oss.isInternalEndpoint()));
  }

  private void applyS3(Map<String, Object> config, StorageProperties.S3Like target) {
    target.setEnabled(true);
    target.setBucket(string(config, "bucket", target.getBucket()));
    target.setEndpoint(string(config, "endpoint", target.getEndpoint()));
    target.setRegion(string(config, "region", target.getRegion()));
    target.setAccessKey(string(config, "accessKey", target.getAccessKey()));
    target.setSecretKey(string(config, "secretKey", target.getSecretKey()));
    target.setCdnHost(string(config, "cdnHost", target.getCdnHost()));
    target.setPathStyle(bool(config, "pathStyle", target.isPathStyle()));
    target.setAccelerate(bool(config, "accelerate", target.isAccelerate()));
    target.setMultipartThresholdMb(longNumber(config, "multipartThresholdMb", target.getMultipartThresholdMb()));
    target.setPartSizeMb(intNumber(config, "partSizeMb", target.getPartSizeMb()));
  }

  private void applyUpyun(Map<String, Object> config) {
    var upyun = storageProperties.getUpyun();
    upyun.setEnabled(true);
    upyun.setBucket(string(config, "bucket", upyun.getBucket()));
    upyun.setOperator(string(config, "operator", upyun.getOperator()));
    upyun.setPassword(string(config, "password", upyun.getPassword()));
    upyun.setEndpoint(string(config, "endpoint", upyun.getEndpoint()));
    upyun.setCdnHost(string(config, "cdnHost", upyun.getCdnHost()));
  }

  private void applyOnedrive(Map<String, Object> config) {
    var onedrive = storageProperties.getOnedrive();
    onedrive.setEnabled(true);
    onedrive.setDriveType(string(config, "driveType", onedrive.getDriveType()));
    onedrive.setTenantId(string(config, "tenantId", onedrive.getTenantId()));
    onedrive.setClientId(string(config, "clientId", onedrive.getClientId()));
    onedrive.setClientSecret(string(config, "clientSecret", onedrive.getClientSecret()));
    onedrive.setDriveId(string(config, "driveId", onedrive.getDriveId()));
    onedrive.setSiteId(string(config, "siteId", onedrive.getSiteId()));
    onedrive.setRefreshToken(string(config, "refreshToken", onedrive.getRefreshToken()));
    onedrive.setRedirectUri(string(config, "redirectUri", onedrive.getRedirectUri()));
    onedrive.setBaseUrl(string(config, "baseUrl", onedrive.getBaseUrl()));
  }

  private String string(Map<String, Object> config, String key, String defaultValue) {
    Object value = config.get(key);
    if (value == null) {
      return defaultValue;
    }
    String text = value.toString().trim();
    return text.isEmpty() ? defaultValue : text;
  }

  private boolean bool(Map<String, Object> config, String key, boolean defaultValue) {
    Object value = config.get(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }
    String normalized = value.toString().trim().toLowerCase(Locale.ROOT);
    if (normalized.isEmpty()) {
      return defaultValue;
    }
    return List.of("true", "1", "yes", "y", "on").contains(normalized);
  }

  private long longNumber(Map<String, Object> config, String key, long defaultValue) {
    Object value = config.get(key);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value.toString().trim());
    } catch (NumberFormatException exception) {
      return defaultValue;
    }
  }

  private int intNumber(Map<String, Object> config, String key, int defaultValue) {
    Object value = config.get(key);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.toString().trim());
    } catch (NumberFormatException exception) {
      return defaultValue;
    }
  }
}
