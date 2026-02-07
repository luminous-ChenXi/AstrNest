package com.imgbed.system;

import com.imgbed.system.dto.SystemConfigResponse;
import com.imgbed.system.dto.SystemInsightResponse;
import com.imgbed.system.dto.UpdateSystemConfigRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/system-config")
@RequiredArgsConstructor
public class SystemConfigController {

  private final SystemConfigService systemConfigService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public SystemConfigResponse load() {
    return systemConfigService.getCurrentConfig();
  }

  @PutMapping
  @PreAuthorize("hasRole('ADMIN')")
  public SystemConfigResponse update(@Valid @RequestBody UpdateSystemConfigRequest request, Authentication authentication) {
    return systemConfigService.updateConfig(request, authentication);
  }

  @GetMapping("/insights")
  @PreAuthorize("hasRole('ADMIN')")
  public SystemInsightResponse insights() {
    return systemConfigService.getInsights();
  }
}
