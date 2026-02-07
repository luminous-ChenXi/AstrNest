package com.imgbed.upload;

import com.imgbed.security.apikey.ApiKey;
import com.imgbed.security.apikey.auth.ApiKeyAuthenticationToken;
import com.imgbed.security.policy.ContentReviewService;
import com.imgbed.security.user.UserAccount;
import com.imgbed.security.user.UserAccountRepository;
import com.imgbed.storage.StorageContext;
import com.imgbed.storage.StorageService;
import com.imgbed.storage.StoredObject;
import com.imgbed.system.SystemConfigService;
import com.imgbed.upload.dto.UploadResponse;
import com.imgbed.upload.record.UploadRecordService;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UploadService {

  private final StorageService storageService;
  private final ContentReviewService contentReviewService;
  private final UploadRecordService uploadRecordService;
  private final UserAccountRepository userAccountRepository;
  private final SystemConfigService systemConfigService;

  public List<UploadResponse> uploadFiles(MultipartFile[] files, Authentication authentication, String clientIp) {
    if (files == null || files.length == 0) {
      throw new IllegalArgumentException("至少需要上传 1 个文件");
    }
    long maxUploadBytes = systemConfigService.currentMaxUploadBytes();
    StorageContext context = StorageContext.localPublicContext();
    UserAccount uploader = resolveUser(authentication);
    enforceUserQuotas(uploader, files);
    ApiKey apiKey = resolveApiKey(authentication);
    return Arrays.stream(files)
        .filter(Objects::nonNull)
        .map(file -> {
          if (file.isEmpty()) {
            throw new IllegalArgumentException("文件 " + safeName(file.getOriginalFilename()) + " 为空");
          }
          if (file.getSize() > maxUploadBytes) {
            String limitText = formatMegabytes(maxUploadBytes);
            throw new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "文件 " + safeName(file.getOriginalFilename()) + " 超过单文件大小限制 " + limitText
            );
          }
          StoredObject stored = storageService.store(file, context);
          String originalName = safeName(file.getOriginalFilename());
          String reviewStatus = contentReviewService.evaluate(file);
          uploadRecordService.recordUserUpload(
              uploader,
              apiKey,
              stored,
              file.getContentType(),
              reviewStatus,
              context,
              clientIp
          );
          return new UploadResponse(
              stored.storedFileName(),
              originalName,
              stored.objectKey(),
              stored.publicUrl(),
              stored.size(),
              Instant.now(),
              reviewStatus,
              true,
              0L,
              0L
          );
        })
        .collect(Collectors.toList());
  }

  private void enforceUserQuotas(UserAccount uploader, MultipartFile[] files) {
    if (uploader == null || files == null || files.length == 0) {
      return;
    }
    int fileCount = (int) Arrays.stream(files).filter(Objects::nonNull).count();
    enforceDailyLimit(uploader, fileCount);
    enforceStorageQuota(uploader, files);
  }

  private void enforceDailyLimit(UserAccount uploader, int incomingFiles) {
    Integer limit = uploader.getDailyUploadLimit();
    if (limit == null || limit <= 0) {
      return;
    }
    long uploadedToday = uploadRecordService.countTodayForUser(uploader.getId());
    if (uploadedToday + incomingFiles > limit) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS,
          "今日上传次数已达上限（" + limit + "），请联系管理员提升配额"
      );
    }
  }

  private void enforceStorageQuota(UserAccount uploader, MultipartFile[] files) {
    Long quotaMb = uploader.getStorageQuotaMb();
    if (quotaMb == null || quotaMb <= 0) {
      return;
    }
    long currentBytes = uploadRecordService.totalSizeForUser(uploader.getId());
    long incomingBytes = Arrays.stream(files)
        .filter(Objects::nonNull)
        .mapToLong(MultipartFile::getSize)
        .sum();
    long quotaBytes = quotaMb * 1024L * 1024L;
    if (currentBytes + incomingBytes > quotaBytes) {
      String limitText = formatMegabytes(quotaBytes);
      throw new ResponseStatusException(
          HttpStatus.PAYLOAD_TOO_LARGE,
          "累计存储空间不足（上限 " + limitText + "），请清理历史图片或申请更大空间"
      );
    }
  }

  private String safeName(String originalFilename) {
    if (!StringUtils.hasText(originalFilename)) {
      return "unnamed";
    }
    return originalFilename;
  }

  private UserAccount resolveUser(Authentication authentication) {
    if (authentication == null) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof UserDetails userDetails) {
      return userAccountRepository.findByUsername(userDetails.getUsername()).orElse(null);
    }
    return null;
  }

  private ApiKey resolveApiKey(Authentication authentication) {
    if (authentication instanceof ApiKeyAuthenticationToken token) {
      return token.getApiKey();
    }
    return null;
  }

  private String formatMegabytes(long bytes) {
    double mb = bytes / (1024d * 1024d);
    return String.format("%.1f MB", mb);
  }
}
