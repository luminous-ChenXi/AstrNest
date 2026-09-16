package com.chenxi.astrnest.upload.media;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "astrnest.video-thumbnail")
public class VideoThumbnailProperties {

  private boolean enabled = true;
  private String ffmpegPath = "ffmpeg";
  private Duration captureOffset = Duration.ofSeconds(1);
  private int quality = 2;
  private long timeoutSeconds = 20;
  private String coverDirectory = "cover";
  /**
   * 当系统平均负载超过该值时跳过缩略图生成，0 或负数表示不限制。
   */
  private double maxLoadAverage = 8.0;
}
