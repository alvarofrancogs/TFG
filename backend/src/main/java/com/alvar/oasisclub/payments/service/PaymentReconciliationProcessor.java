package com.alvar.oasisclub.payments.service;

import com.alvar.oasisclub.payments.entity.StripeProcessedEventEntity;
import com.alvar.oasisclub.payments.repository.StripeProcessedEventRepository;
import com.alvar.oasisclub.reservations.entity.ReservationEntity;
import com.alvar.oasisclub.reservations.entity.ReservationStatus;
import com.alvar.oasisclub.reservations.repository.ReservationRepository;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentReconciliationProcessor {

  private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationProcessor.class);

  private final ReservationRepository reservationRepository;
  private final StripeProcessedEventRepository processedEventRepository;
  private final String stripeSecretKey;

  public PaymentReconciliationProcessor(
      ReservationRepository reservationRepository,
      StripeProcessedEventRepository processedEventRepository,
      @Value("${app.stripe.secret-key:}") String stripeSecretKey
  ) {
    this.reservationRepository = reservationRepository;
    this.processedEventRepository = processedEventRepository;
    this.stripeSecretKey = stripeSecretKey;
  }

  
  @Transactional
  public void reconcileOne(ReservationEntity reservation) throws Exception {
    String sessionId = reservation.getStripeSessionId();

    RequestOptions opts = RequestOptions.builder()
        .setApiKey(stripeSecretKey)
        .setConnectTimeout(5_000)
        .setReadTimeout(10_000)
        .build();

    Session session = Session.retrieve(sessionId, opts);
    String stripeStatus = session.getStatus();         
    String paymentStatus = session.getPaymentStatus(); 

    log.info("[RECONCILIATION] reservation_id={} session={} stripeStatus={} paymentStatus={}",
        reservation.getId(), sessionId, stripeStatus, paymentStatus);

    
    String reconciliationKey = "reconcile:" + sessionId;
    if (processedEventRepository.existsByStripeEventId(reconciliationKey)) {
      log.info("[RECONCILIATION] reservation_id={} already reconciled — skipping", reservation.getId());
      return;
    }

    
    if ("complete".equals(stripeStatus) && "paid".equals(paymentStatus)
        && reservation.getStatus() == ReservationStatus.CONFIRMED) {
      log.info("[RECONCILIATION] reservation_id={} already CONFIRMED — no change", reservation.getId());
      return;
    }

    if ("expired".equals(stripeStatus) && reservation.getStatus() == ReservationStatus.PENDING) {
      
      reservationRepository.delete(reservation);
      log.info("[RECONCILIATION] Reconciled manually: reservation_id={} session={} → deleted (expired)",
          reservation.getId(), sessionId);
    } else if ("complete".equals(stripeStatus) && "paid".equals(paymentStatus)
        && reservation.getStatus() == ReservationStatus.PENDING) {
      
      reservation.setStatus(ReservationStatus.CONFIRMED);
      reservationRepository.save(reservation);
      log.info("[RECONCILIATION] Reconciled manually: reservation_id={} session={} → CONFIRMED (paid)",
          reservation.getId(), sessionId);
    } else {
      log.info("[RECONCILIATION] reservation_id={} stripeStatus={} paymentStatus={} — no applicable transition",
          reservation.getId(), stripeStatus, paymentStatus);
      return;
    }

    
    
    processedEventRepository.save(
        StripeProcessedEventEntity.builder()
            .stripeEventId(reconciliationKey)
            .eventType("reconciliation")
            .processedAt(Instant.now())
            .build()
    );
  }

  
  @Transactional
  public void reconcileRefund(ReservationEntity reservation) throws Exception {
    String sessionId = reservation.getStripeSessionId();

    RequestOptions opts = RequestOptions.builder()
        .setApiKey(stripeSecretKey)
        .setConnectTimeout(5_000)
        .setReadTimeout(10_000)
        .build();

    Session session = Session.retrieve(sessionId, opts);
    String paymentIntentId = session.getPaymentIntent();
    if (paymentIntentId == null || paymentIntentId.isBlank()) {
      return;
    }

    PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId, opts);
    Long amountReceived = paymentIntent.getAmountReceived();
    Long amountRefunded = null;
    if (paymentIntent.getLatestChargeObject() != null) {
      amountRefunded = paymentIntent.getLatestChargeObject().getAmountRefunded();
    }

    if (amountRefunded != null && amountRefunded > 0 && reservation.getRefundedAt() == null) {
      reservation.setRefundedAt(Instant.now());
      reservationRepository.save(reservation);
      log.info("[RECONCILIATION] Refund detected for reservation_id={} session={} amountRefunded={} of {}",
          reservation.getId(), sessionId, amountRefunded, amountReceived);
    }
  }
}
