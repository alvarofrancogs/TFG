package com.alvar.oasisclub.contact;

import com.alvar.oasisclub.common.email.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
@Tag(name = "Contacto", description = "Formulario de contacto para que los visitantes se pongan en contacto con el club")
public class ContactController {

  private final EmailService emailService;

  @PostMapping
  @Operation(
      summary = "Enviar mensaje de contacto",
      description = "Procesa el formulario de contacto: envía el mensaje al email del club y envía una confirmación automática al remitente."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Mensaje enviado correctamente"),
      @ApiResponse(responseCode = "400", description = "Datos del formulario inválidos o campos obligatorios vacíos"),
      @ApiResponse(responseCode = "500", description = "Error al enviar el email")
  })
  public ResponseEntity<Void> sendContact(@Valid @RequestBody ContactRequest request) {
    emailService.sendContactFormToClub(
        request.nombre(),
        request.apellidos(),
        request.email(),
        request.asunto(),
        request.mensaje()
    );
    emailService.sendContactConfirmationToUser(
        request.email(),
        request.nombre()
    );
    return ResponseEntity.ok().build();
  }
}
