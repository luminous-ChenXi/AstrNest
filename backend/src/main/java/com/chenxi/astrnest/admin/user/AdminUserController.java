package com.chenxi.astrnest.admin.user;

import com.chenxi.astrnest.admin.user.dto.AdminUserResponse;
import com.chenxi.astrnest.admin.user.dto.UpdateUserLimitsRequest;
import com.chenxi.astrnest.admin.user.dto.UpdateUserRoleRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

  private final AdminUserService adminUserService;

  @GetMapping
  public List<AdminUserResponse> list() {
    return adminUserService.listUsers();
  }

  @PutMapping("/{id}/limits")
  public AdminUserResponse updateLimits(@PathVariable Long id, @Valid @RequestBody UpdateUserLimitsRequest request) {
    return adminUserService.updateLimits(id, request);
  }

  @PutMapping("/{id}/role")
  public AdminUserResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
    return adminUserService.updateRole(id, request);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    adminUserService.deleteUser(id);
  }
}
