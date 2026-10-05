package com.chenxi.astrnest.common;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 进程内滑动窗口限流器：用于无独立限流基础设施（Redis/网关）部署下的
 * 匿名热点端点（图形验证码获取、邮箱占用检查等）兜底防刷。
 *
 * <p>口径与 {@code SsoAuthController} 的内联限流一致：单机生效、重启清零、多实例各自计数——
 * 分布式部署需换集中式存储，这里只解决「单机被灌爆」的最常见场景。</p>
 */
@Component
public class InMemoryRateLimiter {

  private final Map<String, Deque<Instant>> windows = new ConcurrentHashMap<>();

  /** 指定键在窗口内已达上限返回 true（并拒绝记录）；否则记账并放行 */
  public boolean tryAcquire(String key, int limit, java.time.Duration window) {
    if (key == null || key.isBlank()) {
      return true;
    }
    Instant now = Instant.now();
    Instant threshold = now.minus(window);
    Deque<Instant> hits = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    synchronized (hits) {
      while (!hits.isEmpty() && hits.peekFirst().isBefore(threshold)) {
        hits.pollFirst();
      }
      if (hits.size() >= limit) {
        return true;
      }
      hits.addLast(now);
      return false;
    }
  }

  /** 周期性清理空窗口，防止键空间随伪 IP 无限增长（配合定时任务调用） */
  public void evictIdleBefore(Instant threshold) {
    windows.entrySet().removeIf(entry -> {
      Deque<Instant> hits = entry.getValue();
      synchronized (hits) {
        hits.removeIf(instant -> instant.isBefore(threshold));
        return hits.isEmpty();
      }
    });
  }
}
