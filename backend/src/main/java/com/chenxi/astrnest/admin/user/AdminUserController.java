package com.chenxi.astrnest.admin.user;

import com.chenxi.astrnest.admin.user.dto.AdminUserResponse;
import com.chenxi.astrnest.admin.user.dto.UpdateUserLimitsRequest;
import com.chenxi.astrnest.admin.user.dto.UpdateUserRoleRequest;
import com.chenxi.astrnest.security.totp.TotpAccountService;
import java.util.Map;
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
  private final TotpAccountService totpAccountService;

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

  /**
   * 重置指定用户的 2FA（TOTP）绑定：删除绑定后，登录二步验证开关开启时该用户
   * 下次登录将重新进入强制绑定流程（还原码全部作废）。
   */
  @PutMapping("/{id}/2fa/reset")
  public Map<String, Object> resetTwoFactor(@PathVariable Long id) {
    boolean removed = totpAccountService.resetBinding(id);
    return Map.of("success", removed,
        "message", removed ? "已重置该用户的二步验证绑定" : "该用户尚未绑定二步验证");
  }
}
