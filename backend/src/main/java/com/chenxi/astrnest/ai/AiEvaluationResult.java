package com.chenxi.astrnest.ai;

import java.util.List;

public record AiEvaluationResult(
    AiModerationResult moderationResult,
    List<AiLabel> labels,
    boolean moderationAttempted,
    boolean labelingAttempted,
    String errorMessage,
    TencentAiError error
) {

  public static AiEvaluationResult disabled() {
    return new AiEvaluationResult(null, List.of(), false, false, null, null);
  }

  public static AiEvaluationResult success(AiModerationResult moderationResult, List<AiLabel> labels,
      boolean moderationAttempted, boolean labelingAttempted) {
    return new AiEvaluationResult(
        moderationResult,
        labels == null ? List.of() : labels,
        moderationAttempted,
        labelingAttempted,
        null,
        null
    );
  }

  public static AiEvaluationResult failure(String errorMessage, TencentAiError error,
      boolean moderationAttempted, boolean labelingAttempted) {
    return new AiEvaluationResult(null, List.of(), moderationAttempted, labelingAttempted, errorMessage, error);
  }

  public boolean moderationSucceeded() {
    return moderationAttempted && moderationResult != null;
  }

  public boolean labelingSucceeded() {
    return labelingAttempted && labels != null && !labels.isEmpty();
  }
}
