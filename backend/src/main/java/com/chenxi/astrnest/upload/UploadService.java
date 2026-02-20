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
    return uploadFiles(files, authentication, clientIp, tagNames, null, null);
  }

  public List<UploadResponse> uploadFiles(MultipartFile[] files, Authentication authentication, String clientIp,
      List<String> tagNames, MultipartFile[] videoCovers, List<String> videoCoverMapping) {
    if (files == null || files.length == 0) {
      throw new IllegalArgumentException("至少需要上传 1 个文件");
    }
    long maxImageBytes = systemConfigService.currentMaxUploadBytes();
    long maxVideoBytes = systemConfigService.currentMaxVideoUploadBytes();
    UserAccount uploader = resolveUser(authentication);
    enforceUserQuotas(uploader, files);
    ApiKey apiKey = resolveApiKey(authentication);
    Set<ChenxiTag> baseTags = new LinkedHashSet<>(chenxiTagService.resolveTags(tagNames));

    // 构建视频文件名到封面的映射
    java.util.Map<String, MultipartFile> coverMap = buildCoverMap(videoCovers, videoCoverMapping);

    return Arrays.stream(files)
        .filter(Objects::nonNull)
        .map(file -> handleSingleUpload(file, uploader, apiKey, clientIp, baseTags, maxImageBytes, maxVideoBytes, coverMap))
        .collect(Collectors.toList());
  }

  public UploadBatchResponse uploadFilesWithSkip(MultipartFile[] files, Authentication authentication, String clientIp,
      List<String> tagNames, MultipartFile[] videoCovers, List<String> videoCoverMapping) {
    if (files == null || files.length == 0) {
      throw new IllegalArgumentException("至少需要上传 1 个文件");
    }

    long maxImageBytes = systemConfigService.currentMaxUploadBytes();
    long maxVideoBytes = systemConfigService.currentMaxVideoUploadBytes();
    UserAccount uploader = resolveUser(authentication);
    ApiKey apiKey = resolveApiKey(authentication);
    Set<ChenxiTag> baseTags = new LinkedHashSet<>(chenxiTagService.resolveTags(tagNames));

    // 构建视频文件名到封面的映射
    java.util.Map<String, MultipartFile> coverMap = buildCoverMap(videoCovers, videoCoverMapping);

    List<UploadResponse> uploaded = new java.util.ArrayList<>();
    List<SkippedFileInfo> skipped = new java.util.ArrayList<>();

    for (MultipartFile file : files) {
      if (file == null || file.isEmpty()) {
        continue;
      }

      String fileName = safeName(file.getOriginalFilename());

      // 检查文件大小限制
      MediaInspection inspection = mediaInspector.inspect(file);
      long maxBytes = inspection.category() == MediaCategory.VIDEO ? maxVideoBytes : maxImageBytes;

      if (file.getSize() > maxBytes) {
        String sizeLimit = formatBytes(maxBytes);
        skipped.add(new SkippedFileInfo(fileName, "文件大小超过限制（最大 " + sizeLimit + "）"));
        continue;
      }

      try {
        UploadResponse response = handleSingleUpload(file, uploader, apiKey, clientIp, baseTags,
            maxImageBytes, maxVideoBytes, coverMap);
        uploaded.add(response);
      } catch (Exception e) {
        log.warn("上传文件失败 {}: {}", fileName, e.getMessage());
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
      message = "部分图片大小超限，仅上传了 " + uploaded.size() + " 个符合要求的图片，"
          + skipped.size() + " 个文件被跳过";
    }

    return new UploadBatchResponse(uploaded, skipped, message);
  }

  private java.util.Map<String, MultipartFile> buildCoverMap(MultipartFile[] videoCovers, List<String> videoCoverMapping) {
    java.util.Map<String, MultipartFile> map = new java.util.HashMap<>();
    if (videoCovers == null || videoCovers.length == 0 || videoCoverMapping == null) {
      return map;
    }
    for (int i = 0; i < videoCovers.length && i < videoCoverMapping.size(); i++) {
      String videoName = videoCoverMapping.get(i);
      MultipartFile cover = videoCovers[i];
      if (StringUtils.hasText(videoName) && cover != null && !cover.isEmpty()) {
        map.put(videoName, cover);
      }
    }
    return map;
  }

  private UploadResponse handleSingleUpload(MultipartFile file, UserAccount uploader, ApiKey apiKey, String clientIp,
      Set<ChenxiTag> baseTags, long maxImageBytes, long maxVideoBytes, java.util.Map<String, MultipartFile> coverMap) {
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

    // 处理视频封面：优先使用前端传来的封面，否则使用 FFmpeg 生成
    VideoThumbnailResult thumbnailResult = null;
    if (inspection.category() == MediaCategory.VIDEO) {
      MultipartFile frontendCover = coverMap.get(file.getOriginalFilename());
      if (frontendCover != null && !frontendCover.isEmpty()) {
        thumbnailResult = storeFrontendCover(frontendCover, context);
      }
      if (thumbnailResult == null) {
        VideoThumbnailService.ThumbnailResult ffmpegResult = videoThumbnailService.generateThumbnail(stored);
        if (ffmpegResult != null) {
          thumbnailResult = new VideoThumbnailResult(ffmpegResult.publicUrl(), ffmpegResult.storageRelativeKey());
        }
      }
    }

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
        aiError,
        inspection.width(),
        inspection.height()
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
      return String.format("%.1f KB", bytes / 1024d);
    } else if (bytes < 1024 * 1024 * 1024) {
      return String.format("%.1f MB", bytes / (1024d * 1024d));
    } else {
      return String.format("%.2f GB", bytes / (1024d * 1024d * 1024d));
    }
  }

  /**
   * 存储前端传来的视频封面
   */
  private VideoThumbnailResult storeFrontendCover(MultipartFile coverFile, StorageContext context) {
    try {
      StoredObject coverStored = storageService.store(coverFile, context);
      String coverUrl = publicAssetUrlResolver.resolveStoredObject(coverStored);
      return new VideoThumbnailResult(coverUrl, coverStored.objectKey());
    } catch (Exception e) {
      log.warn("存储前端封面失败: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 视频封面结果记录
   */
  private record VideoThumbnailResult(String publicUrl, String storageRelativeKey) {}
}
