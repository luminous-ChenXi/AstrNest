package com.chenxi.astrnest.upload;

import com.chenxi.astrnest.storage.PublicAssetUrlResolver;
import com.chenxi.astrnest.upload.media.MediaCategory;
import com.chenxi.astrnest.upload.record.UploadRecord;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/embed/video")
@RequiredArgsConstructor
public class VideoEmbedController {

  private final UploadRecordRepository uploadRecordRepository;
  private final PublicAssetUrlResolver publicAssetUrlResolver;

  @GetMapping("/{mediaUuid}")
  public ResponseEntity<String> render(@PathVariable String mediaUuid) {
    UploadRecord record = uploadRecordRepository.findByMediaUuid(mediaUuid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "视频不存在"));
    if (record.getMediaCategory() != MediaCategory.VIDEO) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持视频嵌入");
    }
    String videoUrl = Optional.ofNullable(publicAssetUrlResolver.resolve(record))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "视频链接无效"));
    String poster = resolvePoster(record, videoUrl);
    String mimeType = Optional.ofNullable(record.getContentType()).orElse("video/mp4");
    String displayName = StringUtils.hasText(record.getFileName()) ? record.getFileName() : "AstrNest 视频";
    String escapedVideoUrl = HtmlUtils.htmlEscape(Objects.requireNonNull(videoUrl));
    String escapedPosterUrl = HtmlUtils.htmlEscape(Objects.requireNonNull(poster));
    String escapedMimeType = HtmlUtils.htmlEscape(Objects.requireNonNull(mimeType));
    String escapedFileName = HtmlUtils.htmlEscape(Objects.requireNonNull(displayName));
    String escapedSizeLabel = HtmlUtils.htmlEscape(Objects.requireNonNull(formatSize(record.getSize())));
    String escapedDurationLabel = HtmlUtils.htmlEscape(Objects.requireNonNull(formatDuration(record.getDurationSeconds())));

    String html = """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>%1$s · AstrNest</title>
          <link rel="preconnect" href="https://cdn.jsdelivr.net" />
          <link rel="dns-prefetch" href="https://cdn.jsdelivr.net" />
          <style>
            :root {
              color-scheme: dark;
              font-family: 'Inter', 'Segoe UI', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
            }
            * { box-sizing: border-box; }
            body {
              margin: 0;
              padding: 24px 12px;
              background: transparent;
            }
            .astrnest-embed {
              width: 100%%;
              max-width: 960px;
              margin: 0 auto;
              border-radius: 20px;
              border: 1px solid rgba(255, 255, 255, 0.1);
              background: #070707;
              box-shadow: 0 20px 45px rgba(0, 0, 0, 0.35);
              overflow: hidden;
            }
            .embed-header {
              padding: 12px 18px;
              font-size: 0.72rem;
              letter-spacing: 0.35em;
              text-transform: uppercase;
              color: rgba(255, 255, 255, 0.7);
              border-bottom: 1px solid rgba(255, 255, 255, 0.06);
            }
            .ratio-box {
              position: relative;
              width: 100%%;
              padding-bottom: 56.25%%;
              background: #000;
            }
            .ratio-box video {
              position: absolute;
              inset: 0;
              width: 100%%;
              height: 100%%;
              object-fit: contain;
              background: #000;
              display: block;
            }
            .meta-bar {
              display: flex;
              flex-wrap: wrap;
              gap: 8px 16px;
              padding: 14px 18px 18px;
              color: rgba(255, 255, 255, 0.8);
              font-size: 0.85rem;
            }
            .title {
              flex: 1 1 220px;
              font-weight: 600;
              color: #fff;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }
            .stat {
              font-size: 0.8rem;
              color: rgba(255, 255, 255, 0.65);
            }
            @media (max-width: 640px) {
              body { padding: 16px 10px; }
              .astrnest-embed { border-radius: 16px; }
              .embed-header { font-size: 0.64rem; letter-spacing: 0.25em; }
            }
          </style>
        </head>
        <body>
          <div class="astrnest-embed">
            <div class="embed-header">ASTRNEST · EMBED</div>
            <div class="ratio-box">
              <video controls playsinline preload="metadata" poster="%2$s" aria-label="%1$s">
                <source src="%3$s" type="%4$s" />
                <source src="%3$s" />
                您的浏览器暂不支持 HTML5 视频播放
              </video>
            </div>
            <div class="meta-bar">
              <div class="title">%1$s</div>
              <div class="stat">%5$s</div>
              <div class="stat">%6$s</div>
            </div>
          </div>
        </body>
        </html>
        """.formatted(escapedFileName, escapedPosterUrl, escapedVideoUrl, escapedMimeType, escapedSizeLabel, escapedDurationLabel);

    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
        .contentType(Objects.requireNonNull(MediaType.TEXT_HTML))
        .body(html);
  }

  private String resolvePoster(UploadRecord record, String fallbackVideoUrl) {
    String candidate = record != null ? record.getThumbnailUrl() : null;
    if (!StringUtils.hasText(candidate) || looksLikeVideoUrl(candidate)) {
      return fallbackVideoUrl;
    }
    return candidate;
  }

  private boolean looksLikeVideoUrl(String url) {
    if (!StringUtils.hasText(url)) {
      return false;
    }
    String normalized = url.split("\\?")[0].toLowerCase(Locale.ROOT);
    return normalized.endsWith(".mp4")
        || normalized.endsWith(".mov")
        || normalized.endsWith(".mkv")
        || normalized.endsWith(".avi")
        || normalized.endsWith(".webm")
        || normalized.endsWith(".flv")
        || normalized.endsWith(".ts")
        || normalized.endsWith(".m4v");
  }

  private String formatSize(long bytes) {
    if (bytes <= 0) {
      return "大小未知";
    }
    String[] units = {"B", "KB", "MB", "GB", "TB"};
    double value = bytes;
    int unitIndex = 0;
    while (value >= 1024 && unitIndex < units.length - 1) {
      value /= 1024;
      unitIndex++;
    }
    return "大小 " + String.format("%.2f %s", value, units[unitIndex]);
  }

  private String formatDuration(Integer durationSeconds) {
    if (durationSeconds == null || durationSeconds <= 0) {
      return "时长未知";
    }
    int hours = durationSeconds / 3600;
    int minutes = (durationSeconds % 3600) / 60;
    int seconds = durationSeconds % 60;
    if (hours > 0) {
      return String.format("时长 %d:%02d:%02d", hours, minutes, seconds);
    }
    return String.format("时长 %02d:%02d", minutes, seconds);
  }
}
