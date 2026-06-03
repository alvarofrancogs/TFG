package com.alvar.oasisclub.newsletter;

import com.alvar.oasisclub.clients.entity.ClientEntity;
import com.alvar.oasisclub.clients.repository.ClientRepository;
import com.alvar.oasisclub.common.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsletterService {

  private final EmailService emailService;
  private final ClientRepository clientRepository;

  @Transactional
  public void subscribe(String email) {
    ClientEntity client = clientRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

    if (Boolean.TRUE.equals(client.getNewsletterSubscribed())) {
      throw new IllegalArgumentException("Ya estás suscrito al newsletter");
    }

    client.setNewsletterSubscribed(true);
    clientRepository.save(client);
    emailService.sendNewsletterConfirmationEmail(email);
  }

  @Transactional
  public void unsubscribe(String email) {
    ClientEntity client = clientRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

    if (!Boolean.TRUE.equals(client.getNewsletterSubscribed())) {
      throw new IllegalArgumentException("No estás suscrito al newsletter");
    }

    client.setNewsletterSubscribed(false);
    clientRepository.save(client);
    emailService.sendNewsletterUnsubscribeEmail(email);
  }

  @Transactional(readOnly = true)
  public boolean isSubscribed(String email) {
    return clientRepository.findByEmailIgnoreCase(email)
        .map(c -> Boolean.TRUE.equals(c.getNewsletterSubscribed()))
        .orElse(false);
  }
}
