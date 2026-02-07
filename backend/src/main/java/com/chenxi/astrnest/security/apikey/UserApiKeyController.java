package com.chenxi.astrnest.security.apikey;

import com.chenxi.astrnest.security.apikey.dto.ApiKeyResponse;
import com.chenxi.astrnest.security.apikey.dto.CreateApiKeyRequest;
import com.chenxi.astrnest.security.apikey.dto.CreateApiKeyResponse;
import com.chenxi.astrnest.security.apikey.dto.UpdateApiKeyStatusRequest;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/api-keys")
@PreAuthorize("hasAnyRole('ADMIN','USER')")
@RequiredArgsConstructor
public class UserApiKeyController {

  private final ApiKeyService apiKeyService;
  private final UserAccountRepository userAccountRepository;

  @GetMapping
  public List<ApiKeyResponse> myKeys() {
    UserAccount currentUser = currentUser();
    return apiKeyService.listKeysForOwner(currentUser.getId());
  }

  @PostMapping
  public CreateApiKeyResponse create(@Valid @RequestBody CreateApiKeyRequest request) {
    UserAccount currentUser = currentUser();
    return apiKeyService.createKeyForOwner(request, currentUser.getId());
  }

  @PutMapping("/{id}/status")
  public ApiKeyResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateApiKeyStatusRequest request) {
    UserAccount currentUser = currentUser();
    return apiKeyService.updateStatusForOwner(id, request, currentUser.getId());
  }

  @PostMapping("/{id}/reset")
  public CreateApiKeyResponse reset(@PathVariable Long id) {
    UserAccount currentUser = currentUser();
    return apiKeyService.resetSecretForOwner(id, currentUser.getId());
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    UserAccount currentUser = currentUser();
    apiKeyService.deleteOwned(id, currentUser.getId());
  }

  private UserAccount currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
      throw new BadCredentialsException("未登录或会话已失效");
    }
    return userAccountRepository.findByUsername(userDetails.getUsername())
        .orElseThrow(() -> new BadCredentialsException("未找到用户"));
  }
}
