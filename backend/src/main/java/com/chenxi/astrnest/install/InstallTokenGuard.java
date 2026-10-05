package com.chenxi.astrnest.install;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * 安装向导令牌守卫：全新部署到站长走完向导之间存在窗口期，/api/install/** 的写端点匿名可达，
 * 公网可达的攻击者可抢注第一个用户（自动 ADMIN）接管站点。部署时配置一次性安装令牌
 * （环境变量 {@code CHENXI_INSTALL_TOKEN}）后，向导写操作必须携带
 * {@code X-Chenxi-Install-Token} 请求头；未配置则保持旧行为（本地/内网部署场景兼容）。
 */
@Component
public class InstallTokenGuard {

  public static final String HEADER_NAME = "X-Chenxi-Install-Token";

  private final byte[] expectedToken;

  public InstallTokenGuard(@Value("${chenxi.install.token:}") String configuredToken) {
    this.expectedToken = StringUtils.hasText(configuredToken)
        ? configuredToken.trim().getBytes(StandardCharsets.UTF_8)
        : null;
  }

  /** 是否启用了安装令牌校验 */
  public boolean isRequired() {
    return expectedToken != null;
  }

  /** 向导写端点统一入口：启用令牌时校验请求头（恒定时间比较），未启用则放行 */
  public void check(HttpServletRequest request) {
    if (expectedToken == null) {
      return;
    }
    String provided = request != null ? request.getHeader(HEADER_NAME) : null;
    byte[] providedBytes = StringUtils.hasText(provided)
        ? provided.trim().getBytes(StandardCharsets.UTF_8)
        : new byte[0];
    if (!MessageDigest.isEqual(expectedToken, providedBytes)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "安装向导已启用令牌保护：请在请求头 " + HEADER_NAME + " 中携带部署时配置的安装令牌（CHENXI_INSTALL_TOKEN）");
    }
  }
}
