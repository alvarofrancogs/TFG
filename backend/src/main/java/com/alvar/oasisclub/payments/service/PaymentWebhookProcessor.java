package com.alvar.oasisclub.payments.service;

import com.alvar.oasisclub.payments.entity.StripeProcessedEventEntity;
import com.alvar.oasisclub.payments.repository.StripeProcessedEventRepository;
import com.alvar.oasisclub.reservations.service.ReservationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWebhookProcessor {

  private static final Logger log = LoggerFactory.getLogger(PaymentWebhookProcessor.class);

  
  private static final String EVT_CHECKOUT_COMPLETED  = "checkout.session.completed";
  private static final String EVT_CHECKOUT_EXPIRED    = "checkout.session.expired";
  private static final String EVT_CHARGE_REFUNDED     = "charge.refunded";
  private static final String EVT_PAYMENT_SUCCEEDED   = "payment_intent.succeeded";
  private static final String EVT_PAYMENT_FAILED      = "payment_intent.payment_failed";

  private final ReservationService reservationService;
  private final StripeProcessedEventRepository processedEventRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public PaymentWebhookProcessor(
      ReservationService reservationService,
      StripeProcessedEventRepository processedEventRepository
  ) {
    this.reservationService = reservationService;
    this.processedEventRepository = processedEventRepository;
  }

  
  @Async
  @Transactional
  public void processWebhookAsync(String payload, String eventId, String eventType, long eventTs) {
    try {
      
      
      
      StripeProcessedEventEntity record = StripeProcessedEventEntity.builder()
          .stripeEventId(eventId)
          .eventType(eventType)
          .processedAt(Instant.now())
          .build();
      processedEventRepository.saveAndFlush(record);

      
      JsonNode dataObject = readDataObject(payload);

      switch (eventType) {
        case EVT_CHECKOUT_COMPLETED -> {
          
          String paymentStatus = dataObject.path("payment_status").asText();
          if ("paid".equals(paymentStatus)) {
            String sessionId = extractId(dataObject);
            if (!sessionId.isBlank()) {
              reservationService.confirmByStripeSessionId(sessionId);
            }
          }
        }

        case EVT_PAYMENT_SUCCEEDED -> {
          
          log.info("[WEBHOOK] payment_intent.succeeded event_id={} — covered by checkout.session.completed flow", eventId);
        }

        case EVT_CHECKOUT_EXPIRED -> {
          
          String sessionId = extractId(dataObject);
          if (!sessionId.isBlank()) {
            reservationService.releasePendingStripeReservation(sessionId);
          }
        }

        case EVT_CHARGE_REFUNDED -> {
          
          
          String reservationIdFromMetadata = dataObject.path("metadata").path("reservationId").asText("");
          if (reservationIdFromMetadata.isBlank()) {
             log.warn("[WEBHOOK] charge.refunded event_id={} — no reservationId in metadata, refund timestamp not recorded", eventId);
          } else {
             try {
                 reservationService.markRefundedByReservationId(UUID.fromString(reservationIdFromMetadata), Instant.ofEpochSecond(eventTs));
             } catch (IllegalArgumentException e) {
                 log.warn("[WEBHOOK] charge.refunded event_id={} — invalid reservationId format: {}", eventId, reservationIdFromMetadata);
             }
          }
        }

        case EVT_PAYMENT_FAILED -> {
          
          log.info("[WEBHOOK] payment_intent.payment_failed event_id={} — no state change applied", eventId);
        }

        default ->
          
          log.info("[WEBHOOK] Unhandled event type={} event_id={} — acknowledged without state change", eventType, eventId);
      }

      log.info("[WEBHOOK] Successfully processed event_id={} type={}", eventId, eventType);

    } catch (DataIntegrityViolationException ex) {
      
      log.info("[WEBHOOK] Concurrent duplicate event_id={} committed by another thread — idempotency maintained", eventId);
    } catch (Exception ex) {
      
      log.error("[WEBHOOK] Processing failed for event_id={} type={} timestamp={} — error: {}",
          eventId, eventType, eventTs, ex.getMessage(), ex);
      
      throw new RuntimeException("Webhook processing failed, triggering rollback", ex);
    }
  }

  private JsonNode readDataObject(String payload) {
    try {
      return objectMapper.readTree(payload).path("data").path("object");
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid Stripe webhook payload");
    }
  }

  private String extractId(JsonNode dataObject) {
    return dataObject.path("id").asText("");
  }
}
