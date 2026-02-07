package com.imgbed.gallery;

import com.imgbed.gallery.dto.PublicToggleLikeResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
public class PublicGalleryLikeController {

  private final PublicGalleryLikeService publicGalleryLikeService;

  @PostMapping("/{id}/like")
  public PublicToggleLikeResponse like(@PathVariable Long id, Authentication authentication,
      @RequestHeader(value = "X-Chenxi-Visitor", required = false) String visitorToken,
      @RequestHeader(value = "User-Agent", required = false) String userAgent,
      HttpServletRequest request) {
    return publicGalleryLikeService.like(id, authentication, visitorToken, userAgent, resolveIp(request));
  }

  @DeleteMapping("/{id}/like")
  public PublicToggleLikeResponse unlike(@PathVariable Long id, Authentication authentication,
      @RequestHeader(value = "X-Chenxi-Visitor", required = false) String visitorToken,
      @RequestHeader(value = "User-Agent", required = false) String userAgent,
      HttpServletRequest request) {
    return publicGalleryLikeService.unlike(id, authentication, visitorToken, userAgent, resolveIp(request));
  }

  private String resolveIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
