package com.alvar.oasisclub.payments.service;

import com.alvar.oasisclub.clients.entity.ClientEntity;
import com.alvar.oasisclub.reservations.entity.ReservationEntity;
import com.stripe.exception.StripeException;

public interface StripeCheckoutClient {

  StripeCheckoutSession createReservationCheckoutSession(
      ReservationEntity reservation,
      ClientEntity client,
      long amountInCents,
      String successUrl,
      String cancelUrl
  ) throws StripeException;

  
  void expireSession(String stripeSessionId, String idempotencyKey) throws StripeException;

  
  void refundBySessionId(String stripeSessionId, String idempotencyKey) throws StripeException;

  

  
  @Deprecated
  default void expireSession(String stripeSessionId) throws StripeException {
    expireSession(stripeSessionId, null);
  }

  
  @Deprecated
  default void refundBySessionId(String stripeSessionId) throws StripeException {
    refundBySessionId(stripeSessionId, null);
  }
}
