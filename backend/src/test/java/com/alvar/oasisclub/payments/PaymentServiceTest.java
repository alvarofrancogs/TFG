package com.alvar.oasisclub.payments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alvar.oasisclub.auth.security.AuthenticatedUser;
import com.alvar.oasisclub.clients.entity.ClientEntity;
import com.alvar.oasisclub.clients.service.ClientService;
import com.alvar.oasisclub.common.config.AppCorsProperties;
import com.alvar.oasisclub.common.config.AppFrontendUrlProperties;
import com.alvar.oasisclub.payments.dto.CancelCheckoutSessionRequest;
import com.alvar.oasisclub.payments.dto.CheckoutSessionResponse;
import com.alvar.oasisclub.payments.dto.CreateCheckoutSessionRequest;
import com.alvar.oasisclub.payments.service.PaymentService;
import com.alvar.oasisclub.payments.service.StripeCheckoutClient;
import com.alvar.oasisclub.payments.service.StripeCheckoutSession;
import com.alvar.oasisclub.reservations.dto.CreateReservationRequest;
import com.alvar.oasisclub.reservations.entity.ReservationEntity;
import com.alvar.oasisclub.reservations.entity.ReservationStatus;
import com.alvar.oasisclub.reservations.entity.SportType;
import com.alvar.oasisclub.reservations.exception.ReservationNotFoundException;
import com.alvar.oasisclub.reservations.service.ReservationService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
}