package com.chenxi.astrnest.storage.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageStrategyBootstrap implements ApplicationRunner {

  private final StorageStrategyService storageStrategyService;

  @Override
  public void run(ApplicationArguments args) {
    storageStrategyService.applyActiveProfileOnStartup();
  }
}
