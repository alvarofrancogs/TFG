package com.alvar.oasisclub.newsletter;

import com.alvar.oasisclub.common.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsletterService {

  private final EmailService emailService;

  public void subscribe(String email) {
    emailService.sendNewsletterConfirmationEmail(email);
  }
}
