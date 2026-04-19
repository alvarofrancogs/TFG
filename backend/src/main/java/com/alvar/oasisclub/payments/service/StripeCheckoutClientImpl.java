package com.alvar.oasisclub.payments.service;

import com.alvar.oasisclub.clients.entity.ClientEntity;
import com.alvar.oasisclub.reservations.entity.ReservationEntity;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeCheckoutClientImpl implements StripeCheckoutClient {
}