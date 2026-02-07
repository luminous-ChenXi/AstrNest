package com.imgbed.storage;

import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "imgbed.storage")
public class StorageProperties {

  private StorageStrategy strategy = StorageStrategy.LOCAL;
  private final Local local = new Local();
  private final Oss oss = new Oss();
  private final S3Like cos = new S3Like();
  private final S3Like kodo = new S3Like();
  private final S3Like obs = new S3Like();
  private final S3Like ks3 = new S3Like();
  private final S3Like s3 = new S3Like();
  private final Upyun upyun = new Upyun();
  private final Onedrive onedrive = new Onedrive();

  @Getter
  @Setter
  public static class Local {
    private Path root = defaultRoot();
    private String publicBaseUrl = "/upload";

    Path resolvedRoot() {
      return root == null ? defaultRoot() : root;
    }

    public void setRoot(Path root) {
      if (root == null) {
        this.root = defaultRoot();
        return;
      }
      this.root = root.toAbsolutePath().normalize();
    }
  }

  @Getter
  @Setter
  public static class Oss {
    private boolean enabled;
    private String endpoint;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private String cdnHost;
    private boolean internalEndpoint;
    private boolean enableCname;
  }

  @Getter
  @Setter
  public static class S3Like {
    private boolean enabled;
    private String endpoint;
    private String region;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private boolean pathStyle = true;
    private String cdnHost;
    private boolean accelerate;
    private long multipartThresholdMb = 5120;
    private int partSizeMb = 25;
  }

  @Getter
  @Setter
  public static class Upyun {
    private boolean enabled;
    private String bucket;
    private String operator;
    private String password;
    private String endpoint = "https://v0.api.upyun.com";
    private String cdnHost;
  }

  @Getter
  @Setter
  public static class Onedrive {
    private boolean enabled;
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String driveId;
    private String siteId;
    private String driveType = "personal";
    private String refreshToken;
    private String redirectUri = "https://login.microsoftonline.com/common/oauth2/nativeclient";
    private String baseUrl = "https://graph.microsoft.com/v1.0";
  }

  private static Path defaultRoot() {
    return Paths.get("../storage/upload").toAbsolutePath().normalize();
  }
}
