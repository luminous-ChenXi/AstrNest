package com.chenxi.astrnest.upload;

import com.chenxi.astrnest.ai.AiDecision;
import com.chenxi.astrnest.ai.AiEvaluationResult;
import com.chenxi.astrnest.ai.AiLabel;
import com.chenxi.astrnest.ai.TencentAiError;
import com.chenxi.astrnest.ai.TencentAiService;
import com.chenxi.astrnest.security.apikey.ApiKey;
import com.chenxi.astrnest.security.apikey.auth.ApiKeyAuthenticationToken;
import com.chenxi.astrnest.security.policy.ContentReviewService;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.storage.PublicAssetUrlResolver;
import com.chenxi.astrnest.storage.StorageContext;
import com.chenxi.astrnest.storage.StorageService;
import com.chenxi.astrnest.storage.StoredObject;
import com.chenxi.astrnest.system.SystemConfigService;
import com.chenxi.astrnest.tag.ChenxiTag;
import com.chenxi.astrnest.tag.ChenxiTagService;
import com.chenxi.astrnest.tag.dto.ChenxiTagResponse;
import com.chenxi.astrnest.upload.dto.AiReviewFeedback;
import com.chenxi.astrnest.upload.dto.UploadBatchResponse;
import com.chenxi.astrnest.upload.dto.UploadBatchResponse.SkippedFileInfo;
import com.chenxi.astrnest.upload.dto.UploadResponse;
import com.chenxi.astrnest.upload.media.ChenxiMediaInspector;
import com.chenxi.astrnest.upload.media.ChenxiMediaInspector.MediaInspection;
import com.chenxi.astrnest.upload.media.MediaCategory;
import com.chenxi.astrnest.upload.media.VideoThumbnailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chenxi.astrnest.upload.record.UploadRecord;
import com.chenxi.astrnest.upload.record.UploadRecordService;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

  private final StorageService storageService;
  private final ContentReviewService contentReviewService;
  private final UploadRecordService uploadRecordService;
  private final UserAccountRepository userAccountRepository;
  private final SystemConfigService systemConfigService;
  private final PublicAssetUrlResolver publicAssetUrlResolver;
  private final ChenxiTagService chenxiTagService;
  private final ChenxiMediaInspector mediaInspector;
  private final VideoThumbnailService videoThumbnailService;
  private final TencentAiService tencentAiService;
  private final ObjectMapper objectMapper;

  public List<UploadResponse> uploadFiles(MultipartFile[] files, Authentication authentication, String clientIp,
      List<String> tagNames) {
    if (files == null || files.length == 0) {
      throw new IllegalArgumentException("至少需要上传 1 个文件");
    }
    long maxImageBytes = systemConfigService.currentMaxUploadBytes();
    long maxVideoBytes = systemConfigService.currentMaxVideoUploadBytes();
    UserAccount uploader = resolveUser(authentication);
    enforceUserQuotas(uploader, files);
    ApiKey apiKey = resolveApiKey(authentication);
    Set<ChenxiTag> baseTags = new LinkedHashSet<>(chenxiTagService.resolveTags(tagNames));
    return Arrays.stream(files)
        .filter(Objects::nonNull)
        .map(file -> handleSingleUpload(file, uploader, apiKey, clientIp, baseTags, maxImageBytes, maxVideoBytes))
        .collect(Collectors.toList());
  }

  private UploadResponse handleSingleUpload(MultipartFile file, UserAccount uploader, ApiKey apiKey, String clientIp,
      Set<ChenxiTag> baseTags, long maxImageBytes, long maxVideoBytes) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("文件 " + safeName(file.getOriginalFilename()) + " 为空");
    }
    MediaInspection inspection = mediaInspector.inspect(file);
    mediaInspector.enforceSizeLimit(file, inspection.category(), maxImageBytes, maxVideoBytes);
    String originalName = safeName(file.getOriginalFilename());
    StorageContext context = StorageContext.localPublicContext(mediaInspector.contextMetadata(inspection.category()));
    StoredObject stored = storageService.store(file, context);
    String reviewStatus = contentReviewService.evaluate(file, inspection.category());
    String publicUrl = publicAssetUrlResolver.resolveStoredObject(stored);
    VideoThumbnailService.ThumbnailResult thumbnailResult = inspection.category() == MediaCategory.VIDEO
        ? videoThumbnailService.generateThumbnail(stored)
        : null;
    String thumbnailUrl = inspection.category() == MediaCategory.VIDEO
        ? (thumbnailResult != null ? thumbnailResult.publicUrl() : null)
        : publicUrl;
    String thumbnailStorageKey = inspection.category() == MediaCategory.VIDEO && thumbnailResult != null
        ? thumbnailResult.storageRelativeKey()
        : null;
    String mediaUuid = UUID.randomUUID().toString();
    String embedUrl = inspection.category() == MediaCategory.VIDEO ? buildEmbedUrl(mediaUuid) : null;
    AiDecision aiDecision = null;
    String aiLabelSnapshot = null;
    TencentAiError aiError = null;
    AiReviewFeedback aiReview = null;
    List<AiLabel> aiLabels = List.of();

    Set<ChenxiTag> tagsForRecord = new LinkedHashSet<>(baseTags);
    if (inspection.category() == MediaCategory.IMAGE) {
      AiEvaluationResult evaluationResult = tencentAiService.evaluateImage(publicUrl, stored.objectKey());
      aiLabels = evaluationResult.labels();
      aiDecision = evaluationResult.moderationSucceeded()
          ? evaluationResult.moderationResult().decision()
          : null;
      aiError = evaluationResult.error();
      aiLabelSnapshot = serializeLabels(aiLabels);
      aiReview = AiReviewFeedback.from(aiDecision, aiLabels, aiError);
      if (aiDecision == AiDecision.BLOCK) {
        reviewStatus = "AI_BLOCKED";
        publicUrl = publicAssetUrlResolver.violationPlaceholderUrl();
        thumbnailUrl = publicUrl;
        thumbnailStorageKey = null;
        deleteStoredObjectSilently(stored);
      } else if (aiDecision == AiDecision.REVIEW) {
        reviewStatus = "AI_REVIEW";
      } else if (evaluationResult.errorMessage() != null) {
        log.warn("AI 审核失败（{}）: {}", originalName, evaluationResult.errorMessage());
      }

      if (aiDecision != AiDecision.BLOCK && evaluationResult.labelingSucceeded()) {
        List<String> aiTagNames = aiLabels.stream()
            .map(AiLabel::name)
            .filter(StringUtils::hasText)
            .toList();
        if (!aiTagNames.isEmpty()) {
          tagsForRecord.addAll(chenxiTagService.resolveTags(aiTagNames));
        }
      }
    }
    if (aiReview == null) {
      aiReview = AiReviewFeedback.from(aiDecision, aiLabels, aiError);
    }

    List<ChenxiTagResponse> tagResponses = tagsForRecord.stream()
        .map(chenxiTagService::toResponse)
        .toList();

    UploadRecord savedRecord = uploadRecordService.recordUserUpload(
        uploader,
        apiKey,
        stored,
        publicUrl,
        inspection.contentType(),
        reviewStatus,
        context,
        clientIp,
        tagsForRecord,
        inspection.category(),
        thumbnailUrl,
        thumbnailStorageKey,
        null,
        embedUrl,
        mediaUuid,
        aiDecision,
        aiLabelSnapshot,
        aiError
    );
    String responseThumbnail = StringUtils.hasText(savedRecord.getThumbnailUrl())
        ? savedRecord.getThumbnailUrl()
        : (inspection.category() == MediaCategory.VIDEO ? null : publicUrl);
    String responsePublicUrl = StringUtils.hasText(savedRecord.getPublicUrl()) ? savedRecord.getPublicUrl() : publicUrl;

    return new UploadResponse(
        stored.storedFileName(),
        originalName,
        stored.objectKey(),
        savedRecord.getMediaUuid(),
        savedRecord.getMediaCategory().name(),
        responsePublicUrl,
        responseThumbnail,
        embedUrl,
        savedRecord.getSize(),
        savedRecord.getUploadedAt(),
        savedRecord.getReviewStatus(),
        savedRecord.isPublicAccessible(),
        savedRecord.getLikeCount(),
        savedRecord.getInvokeCount(),
        tagResponses,
        aiReview
    );
  }

  private void enforceUserQuotas(UserAccount uploader, MultipartFile[] files) {
    if (uploader == null || files == null || files.length == 0) {
      return;
    }
    int fileCount = (int) Arrays.stream(files).filter(Objects::nonNull).count();
    enforceTotalLimit(uploader, fileCount);
    enforceStorageQuota(uploader, files);
  }

  private void enforceTotalLimit(UserAccount uploader, int incomingFiles) {
    Integer limit = uploader.getDailyUploadLimit();
    if (limit == null || limit <= 0) {
      return;
    }
    long totalUploaded = uploadRecordService.countTotalForUser(uploader.getId());
    if (totalUploaded + incomingFiles > limit) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS,
          "上传数量已达上限（" + limit + "），请联系管理员提升配额"
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

  private String buildEmbedUrl(String mediaUuid) {
    if (!StringUtils.hasText(mediaUuid)) {
      return null;
    }
    String domain = systemConfigService.currentAssetDomain();
    String prefix = StringUtils.hasText(domain) ? domain : "";
    if (prefix.endsWith("/")) {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    return prefix + "/embed/video/" + mediaUuid;
  }

  private void deleteStoredObjectSilently(StoredObject storedObject) {
    if (storedObject == null || !StringUtils.hasText(storedObject.objectKey())) {
      return;
    }
    try {
      storageService.delete(storedObject.objectKey(), storedObject.providerKey());
    } catch (Exception exception) {
      log.warn("删除存储文件失败 {}：{}", storedObject.objectKey(), exception.getMessage());
    }
  }

  private String serializeLabels(List<AiLabel> labels) {
    if (labels == null || labels.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(labels);
    } catch (JsonProcessingException exception) {
      log.warn("序列化 AI 标签失败：{}", exception.getMessage());
      return null;
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

  private String formatBytes(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    } else if (bytes < 1024 * 1024) {
      return String.format("%.1f KB", bytes / 1024.0);
    } else {
      return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
  }

  /**
   * 上传文件，自动跳过超大文件，继续处理其他文件
   *
   * @param files          文件数组
   * @param authentication 认证信息
   * @param clientIp       客户端IP
   * @param tagNames       标签列表
   * @return 批次上传响应
   */
  public UploadBatchResponse uploadFilesWithSkip(MultipartFile[] files, Authentication authentication,
      String clientIp, List<String> tagNames) {
    if (files == null || files.length == 0) {
      throw new IllegalArgumentException("至少需要上传 1 个文件");
    }

    long maxImageBytes = systemConfigService.currentMaxUploadBytes();
    long maxVideoBytes = systemConfigService.currentMaxVideoUploadBytes();
    UserAccount uploader = resolveUser(authentication);
    enforceUserQuotas(uploader, files);
    ApiKey apiKey = resolveApiKey(authentication);
    Set<ChenxiTag> baseTags = new LinkedHashSet<>(chenxiTagService.resolveTags(tagNames));

    List<UploadResponse> uploaded = new java.util.ArrayList<>();
    List<SkippedFileInfo> skipped = new java.util.ArrayList<>();

    for (MultipartFile file : files) {
      if (file == null) {
        continue;
      }

      String fileName = safeName(file.getOriginalFilename());

      // 检查文件大小，超过限制则跳过
      long fileSize = file.getSize();
      MediaInspection inspection = null;
      try {
        inspection = mediaInspector.inspect(file);
        long maxBytes = inspection.category() == MediaCategory.VIDEO ? maxVideoBytes : maxImageBytes;
        if (fileSize > maxBytes) {
          skipped.add(new SkippedFileInfo(fileName,
              "文件大小 " + formatBytes(fileSize) + " 超过限制 " + formatBytes(maxBytes)));
          continue;
        }
      } catch (Exception e) {
        log.warn("检查文件 {} 大小时出错: {}", fileName, e.getMessage());
        skipped.add(new SkippedFileInfo(fileName, "无法检查文件大小: " + e.getMessage()));
        continue;
      }

      // 尝试上传单个文件
      try {
        UploadResponse response = handleSingleUpload(file, uploader, apiKey, clientIp, baseTags, maxImageBytes,
            maxVideoBytes);
        uploaded.add(response);
      } catch (ResponseStatusException e) {
        // 如果是大小限制错误，记录并跳过
        if (e.getStatusCode() == HttpStatus.PAYLOAD_TOO_LARGE) {
          skipped.add(new SkippedFileInfo(fileName, e.getReason()));
        } else {
          skipped.add(new SkippedFileInfo(fileName, e.getReason() != null ? e.getReason() : "上传失败"));
        }
      } catch (Exception e) {
        log.warn("上传文件 {} 失败: {}", fileName, e.getMessage());
        skipped.add(new SkippedFileInfo(fileName, "上传失败: " + e.getMessage()));
      }
    }

    // 构建人性化提示消息
    String message;
    if (skipped.isEmpty()) {
      message = "全部 " + uploaded.size() + " 个文件上传成功";
    } else if (uploaded.isEmpty()) {
      message = "上传失败，" + skipped.size() + " 个文件未能上传，请检查文件大小或格式";
    } else {
      message = "部分图片大小超限，仅上传了 " + uploaded.size() + " 个符合要求的图片，" + skipped.size() + " 个文件被跳过";
    }

    return new UploadBatchResponse(uploaded, skipped, message);
  }
}
