package com.alvar.oasisclub.common.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alvar.oasisclub.common.config.MailSenderProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import com.alvar.oasisclub.common.email.repository.EmailFailureRepository;

@ExtendWith(MockitoExtension.class)
class EmailDeliveryServiceTest {

  @Mock
  private JavaMailSender mailSender;

  @Mock
  private MailSenderProperties mailSenderProperties;

  @Mock
  private EmailFailureRepository emailFailureRepository;

  @InjectMocks
  private EmailDeliveryService emailDeliveryService;

  @Test
  void deliverUsesOriginalRecipientWhenNoOverride() throws Exception {
    MimeMessage mimeMessage = createMimeMessage();

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(mailSenderProperties.getFromEmail()).thenReturn("club@oasis.com");
    when(mailSenderProperties.getOverrideTo()).thenReturn("");

    emailDeliveryService.deliver("client@example.com", "Oasis Club | Reset Password", "text", "html", "PasswordReset", false);

    ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    MimeMessage sent = messageCaptor.getValue();

    assertEquals("club@oasis.com", ((InternetAddress) sent.getFrom()[0]).getAddress());
    assertEquals("client@example.com", ((InternetAddress) sent.getAllRecipients()[0]).getAddress());
    assertEquals("Oasis Club | Reset Password", sent.getSubject());
  }

  @Test
  void deliverUsesOverrideRecipient() throws Exception {
    MimeMessage mimeMessage = createMimeMessage();

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(mailSenderProperties.getFromEmail()).thenReturn("club@oasis.com");
    when(mailSenderProperties.getOverrideTo()).thenReturn("qa@example.com");

    emailDeliveryService.deliver("client@example.com", "Welcome", "text", "html", "Welcome", false);

    ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    MimeMessage sent = messageCaptor.getValue();

    assertEquals("qa@example.com", ((InternetAddress) sent.getAllRecipients()[0]).getAddress());
  }

  @Test
  void deliverDirectBypassesOverride() throws Exception {
    MimeMessage mimeMessage = createMimeMessage();

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(mailSenderProperties.getFromEmail()).thenReturn("club@oasis.com");
    
    

    emailDeliveryService.deliver("client@example.com", "Direct", "text", "html", "Direct", true);

    ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    MimeMessage sent = messageCaptor.getValue();

    assertEquals("client@example.com", ((InternetAddress) sent.getAllRecipients()[0]).getAddress());
  }

  private MimeMessage createMimeMessage() {
    return new MimeMessage(Session.getInstance(new Properties()));
  }
}
