package com.alvar.oasisclub.newsletter;

import com.alvar.oasisclub.auth.security.AccessControlService;
import com.alvar.oasisclub.auth.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/newsletter")
@RequiredArgsConstructor
@Tag(name = "Newsletter", description = "Gestión de suscripciones al boletín informativo del club")
public class NewsletterController {

  private final NewsletterService newsletterService;
  private final AccessControlService accessControl;

  public record SubscribeRequest(@NotBlank @Email String email) {}

  @PostMapping("/subscribe")
  @Operation(
      summary = "Suscribirse al newsletter",
      description = "Suscribe un email al boletín informativo del club. Si el usuario está autenticado, se utiliza el email de su cuenta; de lo contrario se usa el email proporcionado en el cuerpo."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Suscripción realizada correctamente"),
      @ApiResponse(responseCode = "400", description = "Email inválido o no proporcionado")
  })
  public ResponseEntity<Void> subscribe(
      @Valid @RequestBody SubscribeRequest request,
      Authentication authentication
  ) {
    String email = resolveEmail(authentication, request.email());
    newsletterService.subscribe(email);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/unsubscribe")
  @Operation(
      summary = "Darse de baja del newsletter",
      description = "Cancela la suscripción de un email al boletín informativo. Si el usuario está autenticado, se utiliza el email de su cuenta; de lo contrario se usa el email del cuerpo."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Baja realizada correctamente"),
      @ApiResponse(responseCode = "400", description = "Email inválido o no proporcionado")
  })
  public ResponseEntity<Void> unsubscribe(
      @Valid @RequestBody SubscribeRequest request,
      Authentication authentication
  ) {
    String email = resolveEmail(authentication, request.email());
    newsletterService.unsubscribe(email);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/status")
  @Operation(
      summary = "Consultar estado de suscripción",
      description = "Devuelve 'true' si el usuario autenticado está suscrito al newsletter, o 'false' en caso contrario."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Estado de suscripción obtenido correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado")
  })
  public ResponseEntity<Boolean> status(Authentication authentication) {
    AuthenticatedUser user = accessControl.requireUser(authentication);
    boolean subscribed = newsletterService.isSubscribed(user.email());
    return ResponseEntity.ok(subscribed);
  }

  private String resolveEmail(Authentication authentication, String fallback) {
    if (authentication != null && authentication.isAuthenticated()) {
      try {
        return accessControl.requireUser(authentication).email();
      } catch (Exception ignored) {}
    }
    return fallback;
  }
}
