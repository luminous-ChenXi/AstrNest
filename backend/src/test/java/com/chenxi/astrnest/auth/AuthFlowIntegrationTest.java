package com.chenxi.astrnest.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chenxi.astrnest.chenxi.auth.ChenxiAuthService;
import com.chenxi.astrnest.security.jwt.JwtTokenService;
import com.chenxi.astrnest.security.user.UserAccount;
import com.chenxi.astrnest.security.user.UserAccountRepository;
import com.chenxi.astrnest.system.SystemConfig;
import com.chenxi.astrnest.system.SystemConfigRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

/**
 * 注册/登录链路回归（审计批次二/五）：首个注册用户 ADMIN、用户名唯一、
 * 公开档案不含 email（P0-2）、令牌版本吊销（P0-6）、改密接口自增版本。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ChenxiAuthService chenxiAuthService;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Autowired
  private SystemConfigRepository systemConfigRepository;

  @Autowired
  private JwtTokenService jwtTokenService;

  private void enableRegistration() {
    SystemConfig config = systemConfigRepository.findById(1L)
        .orElseGet(SystemConfig::new);
    config.setRegistrationEnabled(true);
    systemConfigRepository.save(config);
  }

  private UserAccount register(String username, String email) {
    chenxiAuthService.registerUser(email, null, null, username, username, "password123");
    return userAccountRepository.findByUsername(username).orElseThrow();
  }

  @Test
  void firstRegisteredUserIsAdminAndSubsequentUsersAreNormal() {
    enableRegistration();

    UserAccount first = register("alice01", "alice@example.com");
    UserAccount second = register("bob02", "bob@example.com");

    assertThat(first.getRoles()).extracting("name").contains("ADMIN");
    assertThat(second.getRoles()).extracting("name").contains("USER")
        .doesNotContain("ADMIN");
  }

  @Test
  void duplicateUsernameIsRejected() {
    enableRegistration();
    register("carol03", "carol@example.com");

    assertThatThrownBy(() -> register("carol03", "other@example.com"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void publicProfileDoesNotExposeEmail() throws Exception {
    enableRegistration();
    UserAccount user = register("dave04", "dave-private@example.com");

    mockMvc.perform(get("/api/public/users/" + user.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").doesNotExist())
        .andExpect(jsonPath("$.displayName").value("dave04"));
  }

  @Test
  void tokenVersionBumpRevokesOldTokens() throws Exception {
    enableRegistration();
    UserAccount user = register("eve05", "eve@example.com");

    String currentToken = jwtTokenService.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());
    mockMvc.perform(get("/api/user/profile").header("Authorization", "Bearer " + currentToken))
        .andExpect(status().isOk());

    // 令牌版本 +1（模拟改密/找回密码触发吊销）：旧令牌必须被拒绝
    user.setTokenVersion(user.getTokenVersion() + 1);
    userAccountRepository.saveAndFlush(user);

    mockMvc.perform(get("/api/user/profile").header("Authorization", "Bearer " + currentToken))
        .andExpect(status().is4xxClientError());

    // 新版本令牌正常工作
    String newToken = jwtTokenService.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());
    mockMvc.perform(get("/api/user/profile").header("Authorization", "Bearer " + newToken))
        .andExpect(status().isOk());
  }

  @Test
  void changePasswordEndpointIncrementsTokenVersion() throws Exception {
    enableRegistration();
    UserAccount user = register("frank06", "frank@example.com");
    long versionBefore = user.getTokenVersion();
    // 请求前先留档旧哈希：MockMvc 与测试同事务，user 与 reloaded 是同一受管实例
    String passwordHashBefore = user.getPassword();

    String token = jwtTokenService.generateToken(user.getId(), user.getUsername(), versionBefore);
    mockMvc.perform(post("/api/user/security/password")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"currentPassword\":\"password123\",\"newPassword\":\"newpass456\",\"confirmPassword\":\"newpass456\"}"))
        .andExpect(status().isOk());

    UserAccount reloaded = userAccountRepository.findById(user.getId()).orElseThrow();
    assertThat(reloaded.getTokenVersion()).isEqualTo(versionBefore + 1);
    assertThat(reloaded.getPassword()).isNotEqualTo(passwordHashBefore);
  }
}
