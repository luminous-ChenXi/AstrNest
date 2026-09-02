package com.chenxi.astrnest.admin.user;

import com.chenxi.astrnest.admin.user.dto.AdminUserResponse;
import com.chenxi.astrnest.admin.user.dto.UpdateUserLimitsRequest;
import com.chenxi.astrnest.admin.user.dto.UpdateUserRoleRequest;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.security.user.UserRole;
import com.chenxi.astrnest.security.user.UserRoleRepository;
import com.chenxi.astrnest.upload.like.UploadLikeRepository;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import com.chenxi.astrnest.upload.record.dto.UserUsageAggregate;
import com.chenxi.astrnest.user.dto.PublicUserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminUserService {

  private static final Set<String> SUPPORTED_ROLES = Set.of("ADMIN", "USER", "GUEST");

  private final UserAccountRepository userAccountRepository;
  private final UserRoleRepository userRoleRepository;
  private final UploadRecordRepository uploadRecordRepository;
  private final UploadLikeRepository uploadLikeRepository;

  @Transactional
  public AdminUserResponse updateLimits(Long userId, UpdateUserLimitsRequest request) {
    UserAccount user = requireUser(userId);
    user.setDailyUploadLimit(normalizeInteger(request.dailyUploadLimit()));
    user.setStorageQuotaMb(normalizeLong(request.storageQuotaMb()));
    userAccountRepository.save(user);
    return toResponse(user, usageFor(userId));
  }

  @Transactional
  public AdminUserResponse updateRole(Long userId, UpdateUserRoleRequest request) {
    UserAccount user = requireUser(userId);
    String normalizedRole = normalizeRole(request.role());
    enforceAdminCapacity(user, normalizedRole);
    UserRole role = userRoleRepository.findByName(normalizedRole)
        .orElseThrow(() -> new IllegalArgumentException("角色 " + normalizedRole + " 不存在"));
    user.setRoles(new HashSet<>(Set.of(role)));
    userAccountRepository.save(user);
    return toResponse(user, usageFor(userId));
  }

  @Transactional
  public void deleteUser(Long userId) {
    UserAccount user = requireUser(userId);
    if (isAdmin(user)) {
      long adminCount = userAccountRepository.countUsersByRole("ADMIN");
      if (adminCount <= 1) {
        throw new IllegalStateException("至少需要保留一位管理员，无法删除当前账号");
      }
    }
    uploadLikeRepository.deleteByUploadOwnerId(userId);
    uploadLikeRepository.deleteByUserId(userId);
    uploadRecordRepository.deleteByUserId(userId);
    userAccountRepository.delete(Objects.requireNonNull(user));
  }

  @Transactional(readOnly = true)
  public List<AdminUserResponse> listUsers() {
    List<UserAccount> users = userAccountRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    Map<Long, UserUsageAggregate> usageMap = usageMap(users.stream().map(UserAccount::getId).toList());
    return users.stream()
        .map(user -> toResponse(user, usageMap.get(user.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public PublicUserProfileResponse publicProfile(Long userId) {
    UserAccount user = requireUser(userId);
    UserUsageAggregate usage = usageFor(userId);
    return new PublicUserProfileResponse(
        user.getId(),
        user.getDisplayName(),
        user.getEmail(),
        user.getAvatarUrl(),
        user.getSignature(),
        usage.uploadCount(),
        usage.storageBytes(),
        usage.likeCount()
    );
  }

  private void enforceAdminCapacity(UserAccount user, String targetRole) {
    if (isAdmin(user) && !"ADMIN".equals(targetRole)) {
      long adminCount = userAccountRepository.countUsersByRole("ADMIN");
      if (adminCount <= 1) {
        throw new IllegalStateException("至少需要保留一位管理员账号");
      }
    }
  }

  private boolean isAdmin(UserAccount user) {
    return user.getRoles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));
  }

  private Integer normalizeInteger(Integer value) {
    if (value == null || value <= 0) {
      return null;
    }
    return value;
  }

  private Long normalizeLong(Long value) {
    if (value == null || value <= 0) {
      return null;
    }
    return value;
  }

  private String normalizeRole(String role) {
    if (!StringUtils.hasText(role)) {
      throw new IllegalArgumentException("请选择角色");
    }
    String normalized = role.trim().toUpperCase(Locale.ROOT);
    if (!SUPPORTED_ROLES.contains(normalized)) {
      throw new IllegalArgumentException("角色 " + role + " 不受支持");
    }
    return normalized;
  }

  private UserUsageAggregate usageFor(Long userId) {
    return usageMap(List.of(userId)).getOrDefault(userId, new UserUsageAggregate(userId, 0, 0, 0));
  }

  private Map<Long, UserUsageAggregate> usageMap(Collection<Long> userIds) {
    Map<Long, UserUsageAggregate> result = new HashMap<>();
    if (userIds == null || userIds.isEmpty()) {
      return result;
    }
    uploadRecordRepository.aggregateUsageByUserIds(userIds)
        .forEach(aggregate -> result.put(aggregate.userId(), aggregate));
    userIds.stream()
        .filter(id -> !result.containsKey(id))
        .forEach(id -> result.put(id, new UserUsageAggregate(id, 0, 0, 0)));
    return result;
  }

  private AdminUserResponse toResponse(UserAccount user, UserUsageAggregate usage) {
    UserUsageAggregate safeUsage = Optional.ofNullable(usage)
        .orElse(new UserUsageAggregate(user.getId(), 0, 0, 0));
    Set<String> roles = user.getRoles().stream()
        .map(UserRole::getName)
        .collect(Collectors.toSet());
    return new AdminUserResponse(
        user.getId(),
        user.getUsername(),
        user.getDisplayName(),
        user.getEmail(),
        user.getAvatarUrl(),
        user.getSignature(),
        roles,
        user.isActive(),
        user.getCreatedAt(),
        safeUsage.uploadCount(),
        safeUsage.storageBytes(),
        safeUsage.likeCount(),
        user.getDailyUploadLimit(),
        user.getStorageQuotaMb()
    );
  }

  private UserAccount requireUser(Long userId) {
    Long requiredId = Objects.requireNonNull(userId, "用户 ID 不能为空");
    return userAccountRepository.findById(requiredId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
  }
}
