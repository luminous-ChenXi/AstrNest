package com.chenxi.astrnest.album;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/picture")
@RequiredArgsConstructor
@Tag(name = "图集公开访问", description = "通过短链访问图集随机图片")
public class AlbumPublicController {

  private final AlbumService albumService;

  @GetMapping("/{pathSlug}")
  @Operation(summary = "通过短链获取随机图片", description = "访问如 /picture/pc 将随机返回图集中的图片（302重定向）")
  public ResponseEntity<Void> serveRandomImage(
      @Parameter(description = "图集路径标识，如 pc") @PathVariable String pathSlug,
      HttpServletRequest request) {
    return albumService.serveRandomImage(pathSlug, request);
  }
}
