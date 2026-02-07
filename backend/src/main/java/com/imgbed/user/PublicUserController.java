package com.imgbed.user;

import com.imgbed.admin.user.AdminUserService;
import com.imgbed.user.dto.PublicUserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/users")
@RequiredArgsConstructor
public class PublicUserController {

  private final AdminUserService adminUserService;

  @GetMapping("/{id}")
  public PublicUserProfileResponse detail(@PathVariable Long id) {
    return adminUserService.publicProfile(id);
  }
}
