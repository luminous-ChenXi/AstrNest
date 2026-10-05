package com.chenxi.astrnest.chenxi.captcha;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChenxiCaptchaTicketRepository extends JpaRepository<ChenxiCaptchaTicket, String> {

  Optional<ChenxiCaptchaTicket> findByVerificationToken(String verificationToken);

  /** 保鲜清理：删除过期验证码票据（匿名接口每调用写一行，必须定期清理） */
  long deleteByExpiresAtBefore(java.time.Instant threshold);
}
