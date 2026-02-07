package com.imgbed.security.apikey;

import com.imgbed.security.apikey.dto.ApiKeyResponse;
import com.imgbed.security.apikey.dto.CreateApiKeyRequest;
import com.imgbed.security.apikey.dto.CreateApiKeyResponse;
import com.imgbed.security.apikey.dto.UpdateApiKeyQuotaRequest;
import com.imgbed.security.apikey.dto.UpdateApiKeyStatusRequest;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApiKeyController {

  private final ApiKeyService apiKeyService;

  @GetMapping
  public List<ApiKeyResponse> list() {
    return apiKeyService.listKeys();
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
