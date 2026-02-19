package com.chenxi.astrnest.ai;

import com.chenxi.astrnest.system.SystemConfigService;
import com.chenxi.astrnest.system.SystemConfigService.TencentAiSettings;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.ciModel.auditing.AudtingCommonInfo;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingResponse;
import com.qcloud.cos.model.ciModel.image.ImageLabelRequest;
import com.qcloud.cos.model.ciModel.image.ImageLabelResponse;
import com.qcloud.cos.model.ciModel.image.Label;
import com.qcloud.cos.region.Region;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class TencentAiService {

  private static final String DETECT_TYPE = "porn,terrorism,politics,ads,illegal,abuse";
  private static final int MAX_AI_TAGS = 20;

  private final SystemConfigService systemConfigService;

  public AiEvaluationResult evaluateImage(String detectUrl, String dataId) {
    TencentAiSettings settings = systemConfigService.currentTencentAiSettings();
    if (settings == null) {
      return AiEvaluationResult.disabled();
    }
    boolean moderationEnabled = settings.moderationReady();
    boolean labelingEnabled = settings.labelingReady();
    if ((!moderationEnabled && !labelingEnabled) || !isHttpUrl(detectUrl)) {
      return AiEvaluationResult.disabled();
    }

    COSClient client = null;
    try {
      client = buildClient(settings);
      AiModerationResult moderationResult = null;
      if (moderationEnabled) {
        ImageAuditingResponse response = client.imageAuditing(
            buildModerationRequest(settings, detectUrl, dataId));
        moderationResult = parseModeration(response, settings);
      }
      List<AiLabel> labels = List.of();
      if (labelingEnabled) {
        ImageLabelResponse response = client.getImageLabel(
            buildLabelRequest(settings, detectUrl, dataId));
        labels = parseLabels(response, settings);
      }
      return AiEvaluationResult.success(moderationResult, labels, moderationEnabled, labelingEnabled);
    } catch (CosServiceException exception) {
      TencentAiError aiError = buildTencentError(exception);
      log.warn("调用腾讯云智能审核失败（服务端）: code={} msg={} requestId={}", exception.getErrorCode(),
          exception.getErrorMessage(), exception.getRequestId());
      String message = resolveErrorMessage(aiError, exception.getMessage());
      return AiEvaluationResult.failure(message, aiError, moderationEnabled, labelingEnabled);
    } catch (CosClientException exception) {
      TencentAiError aiError = TencentAiError.of(
          "CLIENT_NETWORK_ERROR",
          exception.getMessage(),
          "无法连接腾讯云，请检查网络或密钥配置",
          null,
          null
      );
      log.warn("调用腾讯云智能审核失败（客户端）: {}", exception.getMessage());
      String message = resolveErrorMessage(aiError, exception.getMessage());
      return AiEvaluationResult.failure(message, aiError, moderationEnabled, labelingEnabled);
    } finally {
      if (client != null) {
        client.shutdown();
      }
    }
  }

  private COSClient buildClient(TencentAiSettings settings) {
    COSCredentials credentials = new BasicCOSCredentials(settings.secretId(), settings.secretKey());
    ClientConfig clientConfig = new ClientConfig(new Region(settings.region()));
    return new COSClient(credentials, clientConfig);
  }

  private ImageAuditingRequest buildModerationRequest(TencentAiSettings settings, String detectUrl, String dataId) {
    ImageAuditingRequest request = new ImageAuditingRequest();
    request.setBucketName(settings.bucket());
    request.setObjectKey(StringUtils.hasText(dataId) ? dataId : "ai-detect-placeholder");
    request.setDetectUrl(detectUrl);
    request.setDetectType(DETECT_TYPE);
    request.setLargeImageDetect("1");
    request.setDataId(dataId);
    return request;
  }

  private ImageLabelRequest buildLabelRequest(TencentAiSettings settings, String detectUrl, String dataId) {
    ImageLabelRequest request = new ImageLabelRequest();
    request.setBucketName(settings.bucket());
    request.setObjectKey(StringUtils.hasText(dataId) ? dataId : "ai-label-placeholder");
    request.setDetectUrl(detectUrl);
    request.setScenes(StringUtils.hasText(settings.detectScenes()) ? settings.detectScenes() : null);
    return request;
  }

  private AiModerationResult parseModeration(ImageAuditingResponse response, TencentAiSettings settings) {
    if (response == null) {
      return null;
    }
    List<AiModerationResult.SceneScore> scenes = new ArrayList<>();
    addScene(scenes, "PORN", response.getPornInfo());
    addScene(scenes, "POLITICS", response.getPoliticsInfo());
    addScene(scenes, "TERRORISM", response.getTerroristInfo());
    addScene(scenes, "ADS", response.getAdsInfo());
    addScene(scenes, "TEENAGER", response.getTeenagerInfo());
    int resultCode = parseInt(response.getResult(), 0);
    AiDecision decision = determineDecision(resultCode, scenes, settings);
    return new AiModerationResult(decision, resultCode, scenes, response.getLabel(), response.getSubLabel());
  }

  private List<AiLabel> parseLabels(ImageLabelResponse response, TencentAiSettings settings) {
    if (response == null || response.getRecognitionResult() == null) {
      return List.of();
    }
    List<AiLabel> labels = new ArrayList<>();
    for (Label label : response.getRecognitionResult()) {
      if (label == null || !StringUtils.hasText(label.getName())) {
        continue;
      }
      int confidence = parseInt(label.getConfidence(), 0);
      if (confidence < settings.labelMinConfidence()) {
        continue;
      }
      labels.add(new AiLabel(label.getName(), label.getFirstCategory(), confidence));
      if (labels.size() >= MAX_AI_TAGS) {
        break;
      }
    }
    return labels;
  }

  private void addScene(List<AiModerationResult.SceneScore> scenes, String sceneName, AudtingCommonInfo info) {
    if (info == null) {
      return;
    }
    int hitFlag = parseInt(info.getHitFlag(), 0);
    int score = parseInt(info.getScore(), 0);
    scenes.add(new AiModerationResult.SceneScore(sceneName, hitFlag, score, info.getLabel()));
  }

  private AiDecision determineDecision(int overallResult, List<AiModerationResult.SceneScore> scenes,
      TencentAiSettings settings) {
    boolean confirmedViolation = scenes.stream().anyMatch(scene -> scene.hitFlag() == 1);
    if (overallResult == 1 || confirmedViolation) {
      return AiDecision.BLOCK;
    }
    boolean highRisk = scenes.stream()
        .anyMatch(scene -> scene.hitFlag() == 2 && scene.score() >= settings.moderationBlockConfidence());
    if (highRisk) {
      return AiDecision.BLOCK;
    }
    boolean requiresReview = overallResult == 2 || scenes.stream()
        .anyMatch(scene -> scene.hitFlag() == 2 && scene.score() >= settings.moderationReviewConfidence());
    if (requiresReview) {
      return AiDecision.REVIEW;
    }
    return AiDecision.PASS;
  }

  private int parseInt(String value, int fallback) {
    if (!StringUtils.hasText(value)) {
      return fallback;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException exception) {
      return fallback;
    }
  }

  private boolean isHttpUrl(String url) {
    if (!StringUtils.hasText(url)) {
      return false;
    }
    String normalized = url.trim().toLowerCase();
    return normalized.startsWith("http://") || normalized.startsWith("https://");
  }

  private TencentAiError buildTencentError(CosServiceException exception) {
    String code = exception.getErrorCode();
    String friendly = TencentAiErrorCatalog.friendlyMessage(code).orElse(null);
    return TencentAiError.of(
        code,
        exception.getErrorMessage(),
        friendly,
        exception.getRequestId(),
        exception.getStatusCode()
    );
  }

  private String resolveErrorMessage(TencentAiError error, String fallback) {
    if (error != null && StringUtils.hasText(error.friendlyMessage())) {
      return error.friendlyMessage();
    }
    if (error != null && StringUtils.hasText(error.message())) {
      return error.message();
    }
    return StringUtils.hasText(fallback) ? fallback : "调用腾讯云智能审核失败";
  }
}
