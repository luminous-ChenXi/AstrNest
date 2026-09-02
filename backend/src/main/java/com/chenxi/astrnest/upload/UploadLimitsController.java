package com.chenxi.astrnest.upload;

import com.chenxi.astrnest.system.SystemConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/upload")
public class UploadLimitsController {

  private final SystemConfigService systemConfigService;

  public UploadLimitsController(SystemConfigService systemConfigService) {
    this.systemConfigService = systemConfigService;
  }

  @GetMapping("/limits")
  public ResponseEntity<UploadLimitsResponse> getUploadLimits() {
    long maxUploadBytes = systemConfigService.currentMaxUploadBytes();
    long maxUploadMb = maxUploadBytes / (1024L * 1024L);
    int dailyUploadCountLimit = 50; // 默认值，可以从系统配置中获取
    
    UploadLimitsResponse response = new UploadLimitsResponse(
        maxUploadBytes,
        maxUploadMb,
        dailyUploadCountLimit,
        new String[]{"jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "ico"}
    );
    
    return ResponseEntity.ok(response);
  }

  public record UploadLimitsResponse(
      long maxFileSizeBytes,
      long maxFileSizeMb,
      int maxFilesPerDay,
      String[] allowedTypes
  ) {}
}