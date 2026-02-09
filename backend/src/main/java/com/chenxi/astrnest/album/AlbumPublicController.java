package com.chenxi.astrnest.album;

import com.chenxi.astrnest.album.dto.AlbumFeaturedResponse;
import com.chenxi.astrnest.album.dto.AlbumMediaResponse;
import com.chenxi.astrnest.album.dto.AlbumResponse;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Tag(name = "图集公开访问", description = "通过短链访问图集随机图片")
public class AlbumPublicController {

  private final AlbumService albumService;
  private final UserAccountRepository userAccountRepository;

  private UserAccount resolveViewer() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    String username = authentication.getName();
    return userAccountRepository.findByUsername(username).orElse(null);
  }

  /**
   * 获取图集详情页（图片瀑布流）
   * 路径: /album/{pathSlug}
   */
  @GetMapping("/album/{pathSlug}")
  @Operation(summary = "获取图集详情", description = "访问图集详情页面，显示图集中的所有图片")
  public ResponseEntity<AlbumResponse> getAlbumDetail(
      @Parameter(description = "图集路径标识，如 pc") @PathVariable String pathSlug) {
    return ResponseEntity.ok(albumService.getPublicAlbumDetail(pathSlug, resolveViewer()));
  }

  /**
   * 获取图集中的所有图片
   * 路径: /api/albums/public/{pathSlug}/medias
   */
  @GetMapping("/api/albums/public/{pathSlug}")
  @Operation(summary = "获取公开图集详情", description = "获取公开图集的详细信息")
  public ResponseEntity<AlbumResponse> getPublicAlbumDetail(
      @Parameter(description = "图集路径标识，如 pc") @PathVariable String pathSlug) {
    return ResponseEntity.ok(albumService.getPublicAlbumDetail(pathSlug, resolveViewer()));
  }

  /**
   * 获取图集中的所有图片
   * 路径: /api/albums/public/{pathSlug}/medias
   */
  @GetMapping("/api/albums/public/{pathSlug}/medias")
  @Operation(summary = "获取图集图片列表", description = "获取图集中的所有图片")
  public ResponseEntity<List<AlbumMediaResponse>> getAlbumMedias(
      @Parameter(description = "图集路径标识，如 pc") @PathVariable String pathSlug) {
    return ResponseEntity.ok(albumService.getPublicAlbumMedias(pathSlug, resolveViewer()));
  }

  /**
   * 通过短链获取随机图片（302重定向）
   * 路径: /picture/random/{pathSlug}
   * 注意：此路径添加 /random 前缀以避免与 /album/{pathSlug} 冲突
   */
  @GetMapping("/picture/random/{pathSlug}")
  @Operation(summary = "通过短链获取随机图片", description = "访问如 /picture/random/pc 将随机返回图集中的图片（302重定向）")
  public ResponseEntity<Void> serveRandomImage(
      @Parameter(description = "图集路径标识，如 pc") @PathVariable String pathSlug,
      HttpServletRequest request) {
    return albumService.serveRandomImage(pathSlug, request);
  }

  /**
   * 兼容旧路径：/picture/{pathSlug}
   * 重定向到新路径：/picture/random/{pathSlug}
   */
  @GetMapping("/picture/{pathSlug}")
  @Operation(summary = "兼容旧路径", description = "重定向到 /picture/random/{pathSlug}", deprecated = true)
  public ResponseEntity<Void> serveRandomImageLegacy(
      @Parameter(description = "图集路径标识，如 pc") @PathVariable String pathSlug,
      HttpServletRequest request) {
    return albumService.serveRandomImage(pathSlug, request);
  }

  /**
   * 获取首页Featured图集（最受欢迎的公开图集）
   * 路径: /api/albums/featured
   */
  @GetMapping("/api/albums/featured")
  @Operation(summary = "获取首页Featured图集", description = "获取最受欢迎的公开图集，按图片喜欢数总和排序，最多返回3个")
  public ResponseEntity<List<AlbumFeaturedResponse>> getFeaturedAlbums() {
    return ResponseEntity.ok(albumService.getFeaturedAlbums());
  }
}
