package com.chenxi.astrnest.chenxi.captcha;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChenxiCaptchaTicketRepository extends JpaRepository<ChenxiCaptchaTicket, String> {

  Optional<ChenxiCaptchaTicket> findByVerificationToken(String verificationToken);
}
