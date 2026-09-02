package com.chenxi.astrnest.gallery;

import com.chenxi.astrnest.gallery.dto.PublicGalleryItemResponse;
import com.chenxi.astrnest.gallery.dto.PublicGalleryMetricsResponse;
import com.chenxi.astrnest.gallery.dto.PublicGalleryPageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

  @GetMapping(value = "/public", produces = MediaType.APPLICATION_JSON_VALUE)
  public PublicGalleryPageResponse publicGallery(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "9") int size,
      Authentication authentication,
      @RequestHeader(value = "X-Chenxi-Visitor", required = false) String visitorToken
  ) {
    return publicGalleryService.list(page, size, authentication, visitorToken);
  }

  @GetMapping(value = "/public/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
  public PublicGalleryMetricsResponse metrics() {
    return publicGalleryService.metrics();
  }

  @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
  public PublicGalleryPageResponse searchByTag(
      @RequestParam String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "21") int size,
      Authentication authentication,
      @RequestHeader(value = "X-Chenxi-Visitor", required = false) String visitorToken
  ) {
    return publicGalleryService.searchByTagKeyword(keyword, page, size, authentication, visitorToken);
  }

  @GetMapping(value = "/public/top-liked", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<PublicGalleryItemResponse> getTopLikedImages(
      @RequestParam(defaultValue = "3") int limit
  ) {
    return publicGalleryService.getTopLikedImages(Math.min(Math.max(limit, 1), 10));
  }
}
