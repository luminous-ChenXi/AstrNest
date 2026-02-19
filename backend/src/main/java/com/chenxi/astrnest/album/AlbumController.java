package com.chenxi.astrnest.album;

import com.chenxi.astrnest.album.dto.AddMediaRequest;
import com.chenxi.astrnest.album.dto.AlbumCreateRequest;
import com.chenxi.astrnest.album.dto.AlbumDetailResponse;
import com.chenxi.astrnest.album.dto.AlbumResponse;
import com.chenxi.astrnest.album.dto.AlbumUpdateRequest;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
@Tag(name = "图集管理", description = "用户图集创建、管理和访问接口")
public class AlbumController {

  private final AlbumService albumService;
  private final UserAccountRepository userAccountRepository;

  private UserAccount getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    String username = authentication.getName();
    return userAccountRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
  }

  @PostMapping
  @Operation(summary = "创建图集", security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<AlbumResponse> createAlbum(
      @Valid @RequestBody AlbumCreateRequest request) {
    UserAccount user = getCurrentUser();
    return ResponseEntity.ok(albumService.createAlbum(user, request));
  }

  @GetMapping
  @Operation(summary = "获取我的图集列表", security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<Page<AlbumResponse>> listMyAlbums(
      @PageableDefault(size = 20) Pageable pageable) {
    UserAccount user = getCurrentUser();
    return ResponseEntity.ok(albumService.listUserAlbums(user, pageable));
  }

  @GetMapping("/{albumUuid}")
  @Operation(summary = "获取图集详情", security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<AlbumDetailResponse> getAlbumDetail(
      @PathVariable String albumUuid) {
    UserAccount user = getCurrentUser();
    return ResponseEntity.ok(albumService.getAlbumDetail(user, albumUuid));
  }

  @PutMapping("/{albumUuid}")
  @Operation(summary = "更新图集", security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<AlbumResponse> updateAlbum(
      @PathVariable String albumUuid,
      @Valid @RequestBody AlbumUpdateRequest request) {
    UserAccount user = getCurrentUser();
    return ResponseEntity.ok(albumService.updateAlbum(user, albumUuid, request));
  }

  @DeleteMapping("/{albumUuid}")
  @Operation(summary = "删除图集", security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<Void> deleteAlbum(
      @PathVariable String albumUuid) {
    UserAccount user = getCurrentUser();
    albumService.deleteAlbum(user, albumUuid);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{albumUuid}/medias")
  @Operation(summary = "添加图片到图集", security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<Void> addMediaToAlbum(
      @PathVariable String albumUuid,
      @Valid @RequestBody AddMediaRequest request) {
    UserAccount user = getCurrentUser();
    albumService.addMediaToAlbum(user, albumUuid, request.getMediaUuid());
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{albumUuid}/medias/{mediaUuid}")
  @Operation(summary = "从图集移除图片", security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<Void> removeMediaFromAlbum(
      @PathVariable String albumUuid,
      @PathVariable String mediaUuid) {
    UserAccount user = getCurrentUser();
    albumService.removeMediaFromAlbum(user, albumUuid, mediaUuid);
    return ResponseEntity.noContent().build();
  }

  /**
   * 获取用户可添加到图集的图片列表（排除已在图集中的图片）
   */
  @GetMapping("/{albumUuid}/available-medias")
  @Operation(summary = "获取可添加的图片列表", description = "获取用户已上传但不在当前图集中的图片", security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<Page<com.chenxi.astrnest.album.dto.AvailableMediaResponse>> getAvailableMedias(
      @PathVariable String albumUuid,
      @PageableDefault(size = 20) Pageable pageable) {
    UserAccount user = getCurrentUser();
    return ResponseEntity.ok(albumService.getAvailableMediasForAlbum(user, albumUuid, pageable));
  }
}
