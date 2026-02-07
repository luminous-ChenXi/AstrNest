package com.imgbed.gallery;

import com.imgbed.gallery.dto.PublicGalleryPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
public class PublicGalleryController {

  private final PublicGalleryService publicGalleryService;

  @GetMapping("/public")
  public PublicGalleryPageResponse publicGallery(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "9") int size,
      Authentication authentication,
      @RequestHeader(value = "X-Chenxi-Visitor", required = false) String visitorToken
  ) {
    return publicGalleryService.list(page, size, authentication, visitorToken);
  }
}
