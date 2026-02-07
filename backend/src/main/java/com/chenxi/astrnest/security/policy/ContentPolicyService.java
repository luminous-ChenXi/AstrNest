package com.chenxi.astrnest.security.policy;

import com.chenxi.astrnest.security.dto.ContentPolicyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentPolicyService {

  private final ContentPolicyRepository repository;

  public ContentPolicy currentPolicy() {
    return repository.findById("default")
        .orElseGet(() -> repository.save(new ContentPolicy()));
  }

  @Transactional
  public ContentPolicy update(ContentPolicyRequest request) {
    ContentPolicy policy = currentPolicy();
    policy.setNsfwDetectionEnabled(request.nsfwDetectionEnabled());
    policy.setViolenceDetectionEnabled(request.violenceDetectionEnabled());
    policy.setManualReviewThreshold(request.manualReviewThreshold());
    policy.setWebhookUrl(request.webhookUrl());
    return repository.save(policy);
  }
}
