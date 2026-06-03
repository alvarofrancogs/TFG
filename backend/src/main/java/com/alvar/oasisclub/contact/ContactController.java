package com.alvar.oasisclub.contact;

import com.alvar.oasisclub.common.email.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
public class ContactController {

  private final EmailService emailService;

  @PostMapping
  public ResponseEntity<Void> sendContact(@Valid @RequestBody ContactRequest request) {
    // Correo al club con el mensaje del usuario
    emailService.sendContactFormToClub(
        request.nombre(),
        request.apellidos(),
        request.email(),
        request.asunto(),
        request.mensaje()
    );
    // Correo de confirmación al remitente
    emailService.sendContactConfirmationToUser(
        request.email(),
        request.nombre()
    );
    return ResponseEntity.ok().build();
  }
}
