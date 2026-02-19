package com.chenxi.astrnest.announcement;

import com.chenxi.astrnest.announcement.dto.AnnouncementPageResponse;
import com.chenxi.astrnest.announcement.dto.AnnouncementRequest;
import com.chenxi.astrnest.announcement.dto.AnnouncementResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/admin/announcements", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
public class AnnouncementAdminController {

  private final AnnouncementService announcementService;

  public AnnouncementAdminController(AnnouncementService announcementService) {
    this.announcementService = announcementService;
  }

  @GetMapping
  public AnnouncementPageResponse list(
      @RequestParam(required = false) AnnouncementLevel level,
      @RequestParam(required = false) AnnouncementStatus status,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    return announcementService.listAdmin(level, status, keyword, page, size);
  }

  @GetMapping("/{id}")
  public AnnouncementResponse detail(@PathVariable("id") long id) {
    return announcementService.getAdmin(id);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public AnnouncementResponse create(@Valid @RequestBody AnnouncementRequest request) {
    return announcementService.create(request);
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public AnnouncementResponse update(@PathVariable("id") long id, @Valid @RequestBody AnnouncementRequest request) {
    return announcementService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable("id") long id) {
    announcementService.delete(id);
  }
}
