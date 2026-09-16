package com.chenxi.astrnest.announcement;

import com.chenxi.astrnest.announcement.dto.AnnouncementPageResponse;
import com.chenxi.astrnest.announcement.dto.AnnouncementResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/public/announcements", produces = MediaType.APPLICATION_JSON_VALUE)
public class AnnouncementController {

  private final AnnouncementService announcementService;

  public AnnouncementController(AnnouncementService announcementService) {
    this.announcementService = announcementService;
  }

  @GetMapping
  public AnnouncementPageResponse list(
      @RequestParam(required = false) AnnouncementLevel level,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    return announcementService.listPublic(level, keyword, page, size);
  }

  @GetMapping("/{id}")
  public AnnouncementResponse detail(@PathVariable("id") long id) {
    return announcementService.getPublic(id);
  }
}
