package com.alvar.oasisclub.auth.service;

import com.alvar.oasisclub.auth.repository.PasswordResetTokenRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenCleanupScheduler {

  private static final Logger log = LoggerFactory.getLogger(TokenCleanupScheduler.class);

  private final PasswordResetTokenRepository resetTokenRepository;

  public TokenCleanupScheduler(PasswordResetTokenRepository resetTokenRepository) {
    this.resetTokenRepository = resetTokenRepository;
  }

  
  @Scheduled(cron = "0 0 4 * * *")
  @Transactional
  public void purgeExpiredTokens() {
    int deleted = resetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    log.info("[TOKEN_CLEANUP] Purged {} expired password reset tokens", deleted);
  }
}
