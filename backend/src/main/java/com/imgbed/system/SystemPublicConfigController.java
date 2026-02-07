package com.imgbed.system;

import com.imgbed.system.dto.PublicSystemConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/public-config")
@RequiredArgsConstructor
public class SystemPublicConfigController {

  private final SystemConfigService systemConfigService;

  @GetMapping
  public PublicSystemConfigResponse load() {
    return systemConfigService.getPublicConfig();
  }
}
