package com.chenxi.astrnest.security.apikey;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "astrnest.api-key")
public class ApiKeyProperties {

  private String headerName = "X-API-Key";
  private int defaultDailyQuota = 1000;
  private int defaultPerMinuteQuota = 120;
}
