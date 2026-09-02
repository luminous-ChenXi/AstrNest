package com.chenxi.astrnest.storage.profile;

import com.chenxi.astrnest.storage.profile.dto.CreateStorageStrategyRequest;
import com.chenxi.astrnest.storage.profile.dto.StorageStrategyProfileResponse;
import com.chenxi.astrnest.storage.profile.dto.UpdateStorageStrategyRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/storage/strategies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StorageStrategyAdminController {

  private final StorageStrategyService storageStrategyService;

  @GetMapping
  public List<StorageStrategyProfileResponse> list() {
    return storageStrategyService.listProfiles();
  }

  @PostMapping
  public StorageStrategyProfileResponse create(@Valid @RequestBody CreateStorageStrategyRequest request,
      Authentication authentication) {
    return storageStrategyService.create(request, authentication);
  }

  @PutMapping("/{id}")
  public StorageStrategyProfileResponse update(@PathVariable Long id,
      @Valid @RequestBody UpdateStorageStrategyRequest request,
      Authentication authentication) {
    return storageStrategyService.update(id, request, authentication);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    storageStrategyService.delete(id);
  }

  @PostMapping("/{id}/activate")
  public StorageStrategyProfileResponse activate(@PathVariable Long id, Authentication authentication) {
    return storageStrategyService.activate(id, authentication);
  }
}
