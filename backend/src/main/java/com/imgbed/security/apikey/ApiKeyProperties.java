package com.imgbed.security.apikey;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "imgbed.api-key")
public class ApiKeyProperties {

  private String headerName = "X-API-Key";
  private int defaultDailyQuota = 1000;
}
