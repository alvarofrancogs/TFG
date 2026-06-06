package com.alvar.oasisclub.common.email;

import com.alvar.oasisclub.common.config.MailSenderProperties;
import com.alvar.oasisclub.common.email.entity.EmailFailureEntity;
import com.alvar.oasisclub.common.email.repository.EmailFailureRepository;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailDeliveryService {

  private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);
  private final JavaMailSender mailSender;
  private final MailSenderProperties mailSenderProperties;
  private final EmailFailureRepository emailFailureRepository;

  @Retryable(
      retryFor = Exception.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 5000, multiplier = 2)
  )
  public void deliver(String toEmail, String subject, String plainText, String htmlText, String type, boolean direct) throws Exception {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
    
    String finalRecipient = direct ? toEmail : resolveRecipient(toEmail);

    helper.setFrom(mailSenderProperties.getFromEmail());
    helper.setTo(finalRecipient);
    helper.setSubject(sanitizeSubject(subject));
    helper.setText(plainText, htmlText);

    mailSender.send(message);
    log.info("{} email sent to {} (requested for {})", type, finalRecipient, toEmail);
  }

  @Recover
  public void recover(Exception e, String toEmail, String subject, String plainText, String htmlText, String type, boolean direct) {
    log.error("Failed to send {} email to {} after retries: {}", type, toEmail, e.getMessage(), e);
    try {
      EmailFailureEntity failure = EmailFailureEntity.builder()
          .toEmail(toEmail)
          .emailType(type)
          .subject(subject != null && subject.length() > 150 ? subject.substring(0, 150) : subject)
          .errorMessage(e.getMessage())
          .failedAt(LocalDateTime.now())
          .build();
      emailFailureRepository.save(failure);
    } catch (Exception dbException) {
      log.error("Also failed to save email failure record to DB: {}", dbException.getMessage());
    }
  }

  private String resolveRecipient(String requestedRecipient) {
    String overrideTo = mailSenderProperties.getOverrideTo();
    if (overrideTo != null && !overrideTo.isBlank()) {
      return overrideTo.trim();
    }
    return requestedRecipient;
  }

  private String sanitizeSubject(String subject) {
    if (subject == null) return "";
    return subject.replaceAll("[\\r\\n]", " ");
  }
}
