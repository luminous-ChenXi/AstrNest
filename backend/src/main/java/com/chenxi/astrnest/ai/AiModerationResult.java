package com.chenxi.astrnest.ai;

import java.util.List;

public record AiModerationResult(
    AiDecision decision,
    int resultCode,
    List<SceneScore> sceneScores,
    String label,
    String subLabel
) {

  public record SceneScore(String scene, int hitFlag, int score, String label) {}
}
