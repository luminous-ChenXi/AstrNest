package com.chenxi.astrnest.upload.dto;

import com.chenxi.astrnest.ai.AiDecision;
import com.chenxi.astrnest.ai.AiLabel;
import com.chenxi.astrnest.ai.TencentAiError;
import java.util.List;

public record AiReviewFeedback(
    AiDecision decision,
    List<AiLabel> labels,
    String errorCode,
    String errorMessage,
    String errorRequestId
) {

  public static AiReviewFeedback from(AiDecision decision, List<AiLabel> labels, TencentAiError error) {
    return new AiReviewFeedback(
        decision,
        labels == null ? List.of() : labels,
        error != null ? error.code() : null,
        error != null ? (error.friendlyMessage() != null ? error.friendlyMessage() : error.message()) : null,
        error != null ? error.requestId() : null
    );
  }
}
