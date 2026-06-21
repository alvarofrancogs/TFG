package com.alvar.oasisclub.payments.controller;

import com.alvar.oasisclub.auth.security.AccessControlService;
import com.alvar.oasisclub.auth.security.AuthenticatedUser;
import com.alvar.oasisclub.payments.dto.CancelCheckoutSessionRequest;
import com.alvar.oasisclub.payments.dto.CheckoutSessionResponse;
import com.alvar.oasisclub.payments.dto.CreateCheckoutSessionRequest;
import com.alvar.oasisclub.payments.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@AllArgsConstructor
@Tag(name = "Pagos", description = "Gestión de sesiones de pago Stripe y recepción de webhooks")
public class PaymentController {

  private final PaymentService paymentService;
  private final AccessControlService accessControl;

  @PostMapping("/create-checkout-session")
  @Operation(
      summary = "Crear sesión de pago",
      description = "Crea una sesión de pago en Stripe para que el cliente complete el pago de una reserva. Devuelve la URL a la que redirigir al usuario para completar el pago."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Sesión de pago creada correctamente, se devuelve la URL de Stripe"),
      @ApiResponse(responseCode = "400", description = "Datos de la reserva inválidos"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "500", description = "Error al conectar con la API de Stripe")
  })
  public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
      @Valid @RequestBody CreateCheckoutSessionRequest request,
      @RequestHeader(value = "Origin", required = false) String requestOrigin,
      Authentication authentication
  ) {
    AuthenticatedUser user = accessControl.requireUser(authentication);
    return ResponseEntity.ok(paymentService.createCheckoutSession(request, user, requestOrigin));
  }

  @PostMapping("/cancel-checkout-session")
  @Operation(
      summary = "Cancelar sesión de pago",
      description = "Cancela una sesión de pago de Stripe que aún no ha sido completada, liberando la pista reservada temporalmente."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Sesión de pago cancelada correctamente"),
      @ApiResponse(responseCode = "400", description = "ID de sesión inválido o sesión no cancelable"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "500", description = "Error al conectar con la API de Stripe")
  })
  public ResponseEntity<Void> cancelCheckoutSession(
      @Valid @RequestBody CancelCheckoutSessionRequest request,
      Authentication authentication
  ) {
    AuthenticatedUser user = accessControl.requireUser(authentication);
    paymentService.cancelCheckoutSession(request, user);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/webhook")
  @Operation(
      summary = "Webhook de Stripe",
      description = "Endpoint que recibe notificaciones de eventos de Stripe (checkout completado, pago fallido, etc.). Este endpoint es llamado directamente por Stripe, no por el frontend. Requiere la cabecera 'Stripe-Signature' para verificar la autenticidad del evento."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Evento procesado correctamente"),
      @ApiResponse(responseCode = "400", description = "Firma del webhook inválida o evento malformado")
  })
  public ResponseEntity<Void> webhook(
      @RequestBody String payload,
      @RequestHeader("Stripe-Signature") String signatureHeader
  ) {
    paymentService.handleWebhook(payload, signatureHeader);
    return ResponseEntity.ok().build();
  }
}
