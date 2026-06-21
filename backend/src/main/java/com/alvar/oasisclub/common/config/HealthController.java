package com.alvar.oasisclub.common.config;

import com.alvar.oasisclub.payments.repository.StripeProcessedEventRepository;
import com.stripe.net.RequestOptions;
import com.stripe.model.Balance;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Health Check", description = "Verificación de estado e integraciones de la plataforma")
public class HealthController {

  private static final Logger log = LoggerFactory.getLogger(HealthController.class);

  
  private static final String STRIPE_CONFIGURED_PATH  = "/api/payments/webhook";
  
  private static final String BACKEND_ACTUAL_PATH     = "/api/v1/payments/webhook";

  private final StripeProcessedEventRepository processedEventRepository;
  private final String stripeSecretKey;

  public HealthController(
      StripeProcessedEventRepository processedEventRepository,
      @Value("${app.stripe.secret-key:}") String stripeSecretKey
  ) {
    this.processedEventRepository = processedEventRepository;
    this.stripeSecretKey = stripeSecretKey;
  }

  @GetMapping("/health")
  @Operation(
      summary = "Verificar estado del sistema",
      description = "Verifica el estado del backend, conectividad con Stripe y discrepancias en los webhooks."
  )
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> body = new HashMap<>();

    
    Map<String, Object> stripe = checkStripeConnectivity();
    body.put("stripe", stripe);

    
    Optional<Instant> lastEvent = processedEventRepository
        .findTopByOrderByProcessedAtDesc()
        .map(e -> e.getProcessedAt());

    if (lastEvent.isPresent()) {
      body.put("lastWebhookReceivedAt", lastEvent.get().toString());
    } else {
      body.put("lastWebhookReceivedAt", "No webhook received yet");
    }

    
    boolean pathMatch = STRIPE_CONFIGURED_PATH.equals(BACKEND_ACTUAL_PATH);
    body.put("webhookPathConfiguredInStripe", STRIPE_CONFIGURED_PATH);
    body.put("webhookPathExposedByBackend", BACKEND_ACTUAL_PATH);
    body.put("webhookPathMatch", pathMatch);
    if (!pathMatch) {
      body.put("webhookPathDiscrepancyWarning",
          "The path configured in Stripe (" + STRIPE_CONFIGURED_PATH + ") does not match " +
          "the actual backend endpoint (" + BACKEND_ACTUAL_PATH + "). " +
          "Update the Stripe webhook URL to: https://oasisclub-backend.render.com" + BACKEND_ACTUAL_PATH);
    }

    body.put("status", "UP");
    return ResponseEntity.ok(body);
  }

  private Map<String, Object> checkStripeConnectivity() {
    Map<String, Object> result = new HashMap<>();
    if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
      result.put("status", "UNKNOWN");
      result.put("error", "STRIPE_SECRET_KEY not configured");
      return result;
    }

    try {
      RequestOptions opts = RequestOptions.builder()
          .setApiKey(stripeSecretKey)
          .setConnectTimeout(3_000)  
          .setReadTimeout(5_000)     
          .build();
      Balance.retrieve(opts);
      result.put("status", "UP");
    } catch (Exception ex) {
      
      log.warn("[HEALTH] Stripe connectivity check failed: {}", ex.getMessage());
      result.put("status", "DOWN");
      result.put("error", "Cannot reach Stripe API: " + ex.getClass().getSimpleName());
    }
    return result;
  }
}
