package com.chenxi.astrnest.user.login;

import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.user.dto.LoginHistoryEntry;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserLoginEventService {

  private static final String UNKNOWN = "未知";

  private final UserLoginEventRepository userLoginEventRepository;
  private final UserAccountRepository userAccountRepository;

  public void recordLogin(UserAccount user, HttpServletRequest request) {
    if (user == null || request == null) {
      return;
    }
    UserLoginEvent event = new UserLoginEvent();
    event.setUser(user);
    String ip = resolveClientIp(request);
    event.setIpAddress(ip);
    event.setLocation(resolveLocation(ip));
    String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse(UNKNOWN);
    // 限制 userAgent 长度，防止数据库截断错误
    event.setUserAgent(userAgent.length() > 250 ? userAgent.substring(0, 250) : userAgent);
    userLoginEventRepository.save(event);
    updateNetworkProfile(user, ip, event.getOccurredAt());
  }

  public List<LoginHistoryEntry> latestForUser(Long userId, int limit) {
    return userLoginEventRepository.findByUserIdOrderByOccurredAtDesc(userId, PageRequest.of(0, limit)).stream()
        .map(event -> new LoginHistoryEntry(
            event.getId(),
            event.getOccurredAt(),
            Optional.ofNullable(event.getIpAddress()).orElse(UNKNOWN),
            Optional.ofNullable(event.getLocation()).orElse(UNKNOWN),
            Optional.ofNullable(event.getUserAgent()).orElse(UNKNOWN)
        ))
        .toList();
  }

  private void updateNetworkProfile(UserAccount user, String ip, Instant occurredAt) {
    if (user == null) {
      return;
    }
    user.setLastLoginIp(ip);
    user.setLastLoginAt(occurredAt);
    Deque<String> history = new ArrayDeque<>();
    if (StringUtils.hasText(user.getLoginIpHistory())) {
      for (String token : user.getLoginIpHistory().split("\\|")) {
        if (StringUtils.hasText(token)) {
          history.addLast(token);
        }
      }
    }
    history.addFirst(occurredAt.toString() + "@" + Optional.ofNullable(ip).orElse(UNKNOWN));
    while (history.size() > 10) {
      history.removeLast();
    }
    user.setLoginIpHistory(String.join("|", history));
    userAccountRepository.save(user);
  }

  private String resolveClientIp(HttpServletRequest request) {
    String[] headerNames = {"X-Forwarded-For", "X-Real-IP", "X-Client-IP"};
    for (String header : headerNames) {
      String value = request.getHeader(header);
      if (value != null && !value.isBlank()) {
        return value.split(",")[0].trim();
      }
    }
    return Optional.ofNullable(request.getRemoteAddr()).orElse(UNKNOWN);
  }

  private String resolveLocation(String ipAddress) {
    if (ipAddress == null || ipAddress.isBlank()) {
      return UNKNOWN;
    }
    if (ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.") || ipAddress.startsWith("172.16.")) {
      return "内网";
    }
    if (ipAddress.equals("127.0.0.1")) {
      return "本机";
    }
    return "公网";
  }
}
