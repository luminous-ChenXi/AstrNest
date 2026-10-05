package com.chenxi.astrnest.install;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class InstallTokenGuardTest {

  @Test
  void allowsEverythingWhenTokenNotConfigured() {
    InstallTokenGuard guard = new InstallTokenGuard("");
    MockHttpServletRequest request = new MockHttpServletRequest();
    assertThat(guard.isRequired()).isFalse();
    assertThatCode(() -> guard.check(request)).doesNotThrowAnyException();
  }

  @Test
  void rejectsMissingHeaderWhenConfigured() {
    InstallTokenGuard guard = new InstallTokenGuard("secret-token");
    assertThat(guard.isRequired()).isTrue();
    assertThatThrownBy(() -> guard.check(new MockHttpServletRequest()))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void rejectsWrongToken() {
    InstallTokenGuard guard = new InstallTokenGuard("secret-token");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(InstallTokenGuard.HEADER_NAME, "wrong-token");
    assertThatThrownBy(() -> guard.check(request)).isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void acceptsCorrectTokenAndTrimsWhitespace() {
    InstallTokenGuard guard = new InstallTokenGuard("secret-token");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(InstallTokenGuard.HEADER_NAME, "  secret-token  ");
    assertThatCode(() -> guard.check(request)).doesNotThrowAnyException();
  }
}
