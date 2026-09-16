package com.chenxi.astrnest.storage.profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class StorageStrategyBootstrap implements ApplicationRunner {

  private final StorageStrategyService storageStrategyService;

  @Override
  public void run(ApplicationArguments args) {
    try {
      storageStrategyService.applyActiveProfileOnStartup();
    } catch (Exception exception) {
      // 全新部署（schema 未安装）时 storage_strategy_profiles 表可能尚不存在，
      // 不能让应用启动失败——安装向导建表后由本引导在下次重启补齐。
      log.warn("Skipped storage strategy bootstrap because schema is not ready yet: {}",
          exception.getMessage());
    }
  }
}
