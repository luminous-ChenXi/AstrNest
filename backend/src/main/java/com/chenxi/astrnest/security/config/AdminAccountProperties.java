package com.chenxi.astrnest.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "astrnest.admin")
public class AdminAccountProperties {

  private String username = "admin";
  private String password = "chenxi123";
  private String displayName = "超级管理员";
  private String email = "chenxi@luminouschenxi.com";
}
