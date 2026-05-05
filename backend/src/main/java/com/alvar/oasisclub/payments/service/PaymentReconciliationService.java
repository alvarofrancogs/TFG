package com.alvar.oasisclub.payments.service;

import com.alvar.oasisclub.reservations.entity.ReservationEntity;
import com.alvar.oasisclub.reservations.repository.ReservationRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PaymentReconciliationService {

  private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);

  
  private static final long STALE_HOURS = 72L;

  private final ReservationRepository reservationRepository;
  private final PaymentReconciliationProcessor reconciliationProcessor;
  private final String stripeSecretKey;

  public PaymentReconciliationService(
      ReservationRepository reservationRepository,
      PaymentReconciliationProcessor reconciliationProcessor,
      @Value("${app.stripe.secret-key:}") String stripeSecretKey
  ) {
    this.reservationRepository = reservationRepository;
    this.reconciliationProcessor = reconciliationProcessor;
    this.stripeSecretKey = stripeSecretKey;
  }

  
  @Scheduled(cron = "0 30 3 * * *")
  public void reconcile() {
    log.info("[RECONCILIATION] Starting daily Stripe reconciliation job");

    if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
      log.warn("[RECONCILIATION] STRIPE_SECRET_KEY not configured — skipping");
      return;
    }

    LocalDateTime cutoff = LocalDateTime.now().minusHours(STALE_HOURS);
    var staleReservations = reservationRepository.findStalePendingWithStripeSession(cutoff);

    log.info("[RECONCILIATION] Found {} stale PENDING reservations to reconcile", staleReservations.size());

    for (ReservationEntity reservation : staleReservations) {
      try {
        reconciliationProcessor.reconcileOne(reservation);
      } catch (Exception ex) {
        
        log.error("[RECONCILIATION] Failed to reconcile reservation_id={} stripe_session={} — {}",
            reservation.getId(), reservation.getStripeSessionId(), ex.getMessage(), ex);
      }
    }

    
    var confirmedNoRefund = reservationRepository.findConfirmedWithStripeSessionAndNoRefund();
    log.info("[RECONCILIATION] Checking {} CONFIRMED reservations for missed refunds", confirmedNoRefund.size());

    for (ReservationEntity reservation : confirmedNoRefund) {
      try {
        reconciliationProcessor.reconcileRefund(reservation);
      } catch (Exception ex) {
        log.error("[RECONCILIATION] Failed to reconcile refund for reservation_id={} stripe_session={} — {}",
            reservation.getId(), reservation.getStripeSessionId(), ex.getMessage(), ex);
      }
    }

    log.info("[RECONCILIATION] Daily reconciliation job completed");
  }
}
