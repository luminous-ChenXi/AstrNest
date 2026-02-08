package com.chenxi.astrnest.chenxi.captcha;

import com.chenxi.astrnest.chenxi.captcha.ChenxiCaptchaImageFactory.CaptchaImagePayload;
import com.chenxi.astrnest.chenxi.captcha.dto.ChenxiCaptchaChallengeResponse;
import com.chenxi.astrnest.chenxi.captcha.dto.ChenxiCaptchaVerifyRequest;
import com.chenxi.astrnest.chenxi.captcha.dto.ChenxiCaptchaVerifyResponse;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChenxiCaptchaService {

  private static final long CHALLENGE_EXPIRE_SECONDS = 180;
  private static final long CERTIFICATION_EXPIRE_SECONDS = 300;
  private static final int MAX_ATTEMPTS = 5;

  private final ChenxiCaptchaTicketRepository ticketRepository;
  private final ChenxiCaptchaImageFactory imageFactory = new ChenxiCaptchaImageFactory();

  public ChenxiCaptchaChallengeResponse createChallenge() {
    CaptchaImagePayload payload = imageFactory.createImage();
    ChenxiCaptchaTicket ticket = new ChenxiCaptchaTicket();
    ticket.setId(UUID.randomUUID().toString().replaceAll("-", ""));
    ticket.setExpectedOffset(0d);
    ticket.setTolerance(0d);
    ticket.setCaptchaCode(payload.code());
    ticket.setAttempts(0);
    ticket.setExpiresAt(Instant.now().plusSeconds(CHALLENGE_EXPIRE_SECONDS));
    ticket.setVerificationTokenExpires(Instant.now().plusSeconds(CERTIFICATION_EXPIRE_SECONDS));
    ticketRepository.save(ticket);
    return new ChenxiCaptchaChallengeResponse(
        ticket.getId(),
        payload.imageBase64(),
        payload.width(),
        payload.height(),
        CHALLENGE_EXPIRE_SECONDS
    );
  }

  @Transactional
  public ChenxiCaptchaVerifyResponse verifyChallenge(ChenxiCaptchaVerifyRequest request) {
    ChenxiCaptchaVerifyRequest payload = Objects.requireNonNull(request, "请求体不能为空");
    if (!StringUtils.hasText(payload.captchaCode())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入验证码");
    }
    if (!StringUtils.hasText(payload.captchaId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码会话不存在");
    }
    String captchaId = Objects.requireNonNull(payload.captchaId(), "验证码会话不存在");
    ChenxiCaptchaTicket ticket = ticketRepository.findById(captchaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码不存在或已过期"));
    Instant now = Instant.now();
    if (ticket.getExpiresAt().isBefore(now)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码已过期，请刷新");
    }
    if (ticket.isVerified()
        && !ticket.isCertificationConsumed()
        && ticket.getVerificationToken() != null
        && ticket.getVerificationTokenExpires().isAfter(now)) {
      return new ChenxiCaptchaVerifyResponse(true, ticket.getVerificationToken());
    }
    String submitted = payload.captchaCode().trim();
    if (ticket.getCaptchaCode() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码无效，请刷新");
    }
    if (!ticket.getCaptchaCode().equalsIgnoreCase(submitted)) {
      ticket.setAttempts(ticket.getAttempts() + 1);
      if (ticket.getAttempts() >= MAX_ATTEMPTS) {
        ticket.setExpiresAt(now.minusSeconds(1));
      }
      ticketRepository.save(ticket);
      return new ChenxiCaptchaVerifyResponse(false, null);
    }
    String certificationToken = UUID.randomUUID().toString().replaceAll("-", "");
    ticket.setVerified(true);
    ticket.setCaptchaCode(null);
    ticket.setVerificationToken(certificationToken);
    ticket.setVerificationTokenExpires(now.plusSeconds(CERTIFICATION_EXPIRE_SECONDS));
    ticket.setVerifiedAt(now);
    ticketRepository.save(ticket);
    return new ChenxiCaptchaVerifyResponse(true, certificationToken);
  }

  @Transactional
  public void consumeCertificationOrThrow(String certificationToken) {
    String token = Objects.requireNonNull(certificationToken, "认证令牌不能为空");
    ChenxiCaptchaTicket ticket = ticketRepository.findByVerificationToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "人机验证尚未完成"));
    Instant now = Instant.now();
    if (!ticket.isVerified() || ticket.isCertificationConsumed()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请完成有效的人机验证");
    }
    if (ticket.getVerificationTokenExpires().isBefore(now)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "人机验证已失效，请重新输入图形验证码");
    }
    ticket.setCertificationConsumed(true);
    ticketRepository.save(ticket);
  }
}
