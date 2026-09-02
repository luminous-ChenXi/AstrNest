package com.chenxi.astrnest.security.apikey;

import com.chenxi.astrnest.security.apikey.dto.ApiKeyDashboardResponse;
import com.chenxi.astrnest.security.apikey.dto.ApiKeyOwnerSummary;
import com.chenxi.astrnest.security.apikey.dto.ApiKeyResponse;
import com.chenxi.astrnest.security.apikey.dto.CreateApiKeyRequest;
import com.chenxi.astrnest.security.apikey.dto.CreateApiKeyResponse;
import com.chenxi.astrnest.security.apikey.dto.UpdateApiKeyQuotaRequest;
import com.chenxi.astrnest.security.apikey.dto.UpdateApiKeyStatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApiKeyController {

  private final ApiKeyService apiKeyService;

  @GetMapping
  public List<ApiKeyResponse> list(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Long ownerId,
      @RequestParam(required = false) Boolean activeOnly) {
    return apiKeyService.listKeys(search, ownerId, activeOnly);
  }

  @GetMapping("/dashboard")
  public ApiKeyDashboardResponse dashboard() {
    return apiKeyService.dashboard();
  }

  @GetMapping("/owners")
  public List<ApiKeyOwnerSummary> owners() {
    return apiKeyService.ownerSummaries();
  }

  @PostMapping
  public CreateApiKeyResponse create(@Valid @RequestBody CreateApiKeyRequest request) {
    return apiKeyService.createKey(request);
  }

  @PutMapping("/{id}/status")
  public ApiKeyResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateApiKeyStatusRequest request) {
    return apiKeyService.updateStatus(id, request);
  }

  @PutMapping("/{id}/quota")
  public ApiKeyResponse updateQuota(@PathVariable Long id, @Valid @RequestBody UpdateApiKeyQuotaRequest request) {
    return apiKeyService.updateQuota(id, request);
  }

  @PostMapping("/{id}/reset")
  public CreateApiKeyResponse reset(@PathVariable Long id) {
    return apiKeyService.resetSecret(id);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    apiKeyService.delete(id);
  }
}
