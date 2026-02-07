package com.imgbed.admin.upload;

import com.imgbed.admin.upload.dto.AdminUpdateViolationRequest;
import com.imgbed.admin.upload.dto.AdminUpdateVisibilityRequest;
import com.imgbed.admin.upload.dto.AdminUploadItemResponse;
import com.imgbed.admin.upload.dto.AdminUploadPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/uploads")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUploadController {

  private final AdminUploadService adminUploadService;


  @GetMapping
  public AdminUploadPageResponse uploads(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Boolean violation,
      @RequestParam(required = false) Boolean publicAccessible,
      @RequestParam(required = false) String storageProvider) {
    return adminUploadService.searchUploads(search, violation, publicAccessible, storageProvider, page, size);
  }

  @PutMapping("/{id}/visibility")
  public AdminUploadItemResponse updateVisibility(@PathVariable long id,
      @Valid @RequestBody AdminUpdateVisibilityRequest request) {
    return adminUploadService.updateVisibility(id, Boolean.TRUE.equals(request.publicAccessible()));
  }

  @PutMapping("/{id}/violation")
  public AdminUploadItemResponse updateViolation(@PathVariable long id,
      @Valid @RequestBody AdminUpdateViolationRequest request) {
    return adminUploadService.updateViolation(id, Boolean.TRUE.equals(request.violation()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    adminUploadService.deleteRecord(id);
  }
}
