package com.alvar.oasisclub.reservations.service;

import com.alvar.oasisclub.clients.entity.ClientEntity;
import com.alvar.oasisclub.clients.service.ClientService;
import com.alvar.oasisclub.common.email.EmailService;
import com.alvar.oasisclub.courts.repository.CourtRepository;
import com.alvar.oasisclub.payments.service.StripeCheckoutClient;
import com.alvar.oasisclub.reservations.dto.AvailabilitySlotResponse;
import com.alvar.oasisclub.reservations.dto.CreateMaintenanceBlockRequest;
import com.alvar.oasisclub.reservations.dto.CreateReservationRequest;
import com.alvar.oasisclub.reservations.dto.ReservationResponse;
import com.alvar.oasisclub.reservations.entity.ReservationEntity;
import com.alvar.oasisclub.reservations.entity.ReservationStatus;
import com.alvar.oasisclub.reservations.entity.SportType;
import com.alvar.oasisclub.reservations.exception.ReservationNotFoundException;
import com.alvar.oasisclub.reservations.mapper.ReservationMapper;
import com.alvar.oasisclub.reservations.repository.ReservationRepository;
import com.alvar.oasisclub.schedule.service.ScheduleSlotService;
import com.stripe.exception.StripeException;
import com.alvar.oasisclub.common.exception.StripeOperationFailedException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ReservationService {

  private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

  private final ReservationRepository reservationRepository;
  private final ReservationMapper reservationMapper;
  private final CourtRepository courtRepository;
  private final ScheduleSlotService scheduleSlotService;
  private final ClientService clientService;
  private final EmailService emailService;
  private final StripeCheckoutClient stripeCheckoutClient;

  @Transactional(readOnly = true)
  public List<ReservationResponse> getReservations(String sport, String status, LocalDate date) {
    SportType parsedSport = sport == null ? null : SportType.valueOf(sport.toUpperCase());
    ReservationStatus parsedStatus = status == null ? null : ReservationStatus.valueOf(status.toUpperCase());

    List<ReservationEntity> entities;
    if (date != null) {
      entities = reservationRepository.findByReservationDateOrderByReservationTimeAsc(date);
    } else {
      entities = reservationRepository.findAllByOrderByReservationDateDescReservationTimeDesc();
    }

    return entities.stream()
        .filter(entity -> parsedSport == null || entity.getSport() == parsedSport)
        .filter(entity -> parsedStatus == null || entity.getStatus() == parsedStatus)
        .map(reservationMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ReservationEntity> getReservationsByClient(UUID clientId) {
    return reservationRepository.findByClientIdOrderByReservationDateDescReservationTimeDesc(clientId);
  }

  @Transactional(readOnly = true)
  public List<ReservationResponse> getReservationsByClient(
      UUID clientId,
      String sport,
      String status,
      LocalDate date
  ) {
    SportType parsedSport = sport == null ? null : SportType.valueOf(sport.toUpperCase());
    ReservationStatus parsedStatus = status == null ? null : ReservationStatus.valueOf(status.toUpperCase());

    return reservationRepository.findByClientIdOrderByReservationDateDescReservationTimeDesc(clientId).stream()
        .filter(entity -> parsedSport == null || entity.getSport() == parsedSport)
        .filter(entity -> parsedStatus == null || entity.getStatus() == parsedStatus)
        .filter(entity -> date == null || entity.getReservationDate().equals(date))
        .map(reservationMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public ReservationEntity getEntityById(UUID id) {
    return reservationRepository.findById(id)
        .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
  }

  @Transactional
  public ReservationResponse createReservation(CreateReservationRequest request) {
    ReservationEntity saved = createReservationEntity(request, ReservationStatus.PENDING);
    return reservationMapper.toResponse(saved);
  }

  @Transactional
  public ReservationResponse createConfirmedReservation(CreateReservationRequest request) {
    ReservationEntity saved = createReservationEntity(request, ReservationStatus.CONFIRMED);
    sendReservationConfirmedEmail(saved);
    return reservationMapper.toResponse(saved);
  }

  @Transactional
  public ReservationEntity createPendingReservationForPayment(CreateReservationRequest request) {
    return createReservationEntity(request, ReservationStatus.PENDING);
  }

  private ReservationEntity createReservationEntity(CreateReservationRequest request, ReservationStatus status) {
    com.alvar.oasisclub.courts.entity.CourtEntity court = requireActiveCourt(request.getCourtId());
    if (court.getSport() != request.getSport()) {
      throw new IllegalArgumentException("El deporte de la pista no coincide con la reserva");
    }

    ensureSlotAvailable(court.getId(), request.getDate(), request.getTime());

    try {
      ReservationEntity reservation = reservationMapper.fromCreateRequest(request, court);
      reservation.setStatus(status);
      return reservationRepository.saveAndFlush(reservation);
    } catch (DataIntegrityViolationException ex) {
      throw new IllegalArgumentException("La franja seleccionada no está disponible");
    }
  }

  @Transactional(readOnly = true)
  public long countActiveClientReservationsByDay(UUID clientId, LocalDate date) {
    return reservationRepository.findByClientIdAndReservationDateOrderByReservationTimeAsc(clientId, date).stream()
        .filter(this::blocksAvailability)
        .count();
  }

  @Transactional
  public ReservationResponse createMaintenanceBlock(CreateMaintenanceBlockRequest request) {
    com.alvar.oasisclub.courts.entity.CourtEntity court = requireActiveCourt(request.getCourtId());
    if (court.getSport() != request.getSport()) {
      throw new IllegalArgumentException("El deporte de la pista no coincide con el mantenimiento");
    }

    var existingOpt = reservationRepository.findByCourt_IdAndReservationDateAndReservationTime(court.getId(), request.getDate(), request.getTime());

    if (existingOpt.isPresent()) {
      ReservationEntity existing = existingOpt.get();
      if (existing.getStatus() == ReservationStatus.MAINTENANCE) {
        throw new IllegalArgumentException("Esta franja ya está en mantenimiento");
      }
      
      if (existing.getStatus() == ReservationStatus.CONFIRMED && existing.getStripeSessionId() != null && !existing.getStripeSessionId().isBlank()) {
        try { stripeCheckoutClient.refundBySessionId(existing.getStripeSessionId()); } catch (Exception ignored) {}
      } else if (existing.getStatus() == ReservationStatus.PENDING && existing.getStripeSessionId() != null && !existing.getStripeSessionId().isBlank()) {
        try { stripeCheckoutClient.expireSession(existing.getStripeSessionId()); } catch (Exception ignored) {}
      }

      boolean refunded = existing.getStatus() == ReservationStatus.CONFIRMED
          && existing.getStripeSessionId() != null
          && !existing.getStripeSessionId().isBlank();
      sendMaintenanceCancellationEmail(existing, refunded);

      existing.setClientId(null);
      existing.setUserName("Mantenimiento");
      existing.setStatus(ReservationStatus.MAINTENANCE);
      existing.setStripeSessionId(null);
      reservationRepository.saveAndFlush(existing);
      return reservationMapper.toResponse(existing);
    }

    try {
      ReservationEntity saved = reservationRepository.saveAndFlush(reservationMapper.fromMaintenanceRequest(request, court));
      return reservationMapper.toResponse(saved);
    } catch (DataIntegrityViolationException ex) {
      log.error("Error al crear bloqueo de mantenimiento: {}", ex.getMessage());
      throw new IllegalArgumentException("La franja seleccionada no está disponible");
    }
  }

  
  @Transactional
  public void deleteReservation(UUID id) {
    ReservationEntity reservation = getEntityById(id);
    cancelAndRefund(reservation);
  }

  
  @Transactional
  public void cancelAndRefund(ReservationEntity reservation) {
    String sessionId = reservation.getStripeSessionId();

    
    
    if (sessionId == null || sessionId.isBlank()
        || reservation.getStatus() == ReservationStatus.MAINTENANCE) {
      reservationRepository.delete(reservation);
      sendReservationCancelledEmail(reservation);
      return;
    }

    
    String idempotencyKey = "cancel-" + reservation.getId().toString();

    
    try {
      if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
        stripeCheckoutClient.refundBySessionId(sessionId, idempotencyKey);
        log.info("Stripe refund requested for reservation {} sessionId {} idempotencyKey {}",
            reservation.getId(), sessionId, idempotencyKey);
      } else if (reservation.getStatus() == ReservationStatus.PENDING) {
        stripeCheckoutClient.expireSession(sessionId, idempotencyKey);
        log.info("Stripe session expiration requested for reservation {} sessionId {} idempotencyKey {}",
            reservation.getId(), sessionId, idempotencyKey);
      }
    } catch (StripeException ex) {
      
      log.error("Stripe operation failed for reservation {} sessionId {} idempotencyKey {} — cancellation NOT completed: {}",
          reservation.getId(), sessionId, idempotencyKey, ex.getMessage(), ex);
      throw new StripeOperationFailedException(
          "La cancelación no se ha completado porque Stripe no respondió correctamente. Puede reintentarse.", ex);
    }

    
    try {
      reservationRepository.delete(reservation);
    } catch (Exception ex) {
      
      log.error("[COMPENSATION REQUIRED] Stripe operation completed but DB delete FAILED " +
              "for reservation_id={} stripeSessionId={} idempotencyKey={} — manual intervention needed",
          reservation.getId(), sessionId, idempotencyKey, ex);
      throw ex;
    }

    
    sendReservationCancelledEmail(reservation);
  }

  
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cancelAndRefundIsolated(ReservationEntity reservation) {
    cancelAndRefund(reservation);
  }

  @Transactional
  public void saveStripeSessionId(UUID reservationId, String stripeSessionId) {
    ReservationEntity reservation = getEntityById(reservationId);
    reservation.setStripeSessionId(stripeSessionId);
  }

  @Transactional
  public void confirmByStripeSessionId(String stripeSessionId) {
    reservationRepository.findByStripeSessionId(stripeSessionId)
        .ifPresent(reservation -> {
          if (reservation.getStatus() == ReservationStatus.PENDING) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            sendReservationConfirmedEmail(reservation);
          }
        });
  }

  @Transactional
  public void releasePendingStripeReservation(String stripeSessionId) {
    reservationRepository.findByStripeSessionId(stripeSessionId)
        .filter(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
        .ifPresent(reservationRepository::delete);
  }

  @Transactional
  public void markRefundedByReservationId(UUID reservationId, Instant refundedAt) {
    reservationRepository.findById(reservationId)
        .ifPresent(reservation -> {
          reservation.setRefundedAt(refundedAt);
          log.info("Marked reservation {} as refunded at {}", reservation.getId(), refundedAt);
        });
  }

  @Transactional(readOnly = true)
  public ReservationEntity getByStripeSessionIdAndClientId(String stripeSessionId, UUID clientId) {
    return reservationRepository.findByStripeSessionIdAndClientId(stripeSessionId, clientId)
        .orElseThrow(() -> new ReservationNotFoundException("Reservation not found"));
  }

  @Transactional(readOnly = true)
  public List<AvailabilitySlotResponse> getAvailability(UUID courtId, LocalDate date) {
    List<String> times = scheduleSlotService.getAllSlots();
    List<ReservationEntity> dayReservations = reservationRepository
        .findByCourt_IdAndReservationDateOrderByReservationTimeAsc(courtId, date);

    List<String> reservedTimes = dayReservations.stream()
        .filter(this::blocksAvailability)
        .map(r -> r.getReservationTime().toString().substring(0, 5))
        .toList();

    List<AvailabilitySlotResponse> result = new ArrayList<>();
    for (String time : times) {
      result.add(new AvailabilitySlotResponse(time, !reservedTimes.contains(time)));
    }
    return result;
  }

  private com.alvar.oasisclub.courts.entity.CourtEntity requireActiveCourt(UUID courtId) {
    com.alvar.oasisclub.courts.entity.CourtEntity court = courtRepository.findById(courtId)
        .orElseThrow(() -> new IllegalArgumentException("Pista no encontrada"));
    if (Boolean.FALSE.equals(court.getIsActive())) {
      throw new IllegalArgumentException("La pista está inactiva");
    }
    return court;
  }

  private void ensureSlotAvailable(UUID courtId, LocalDate date, LocalTime time) {
    if (reservationRepository.existsByCourt_IdAndReservationDateAndReservationTime(courtId, date, time)) {
      throw new IllegalArgumentException("La franja seleccionada no está disponible");
    }
  }

  private boolean blocksAvailability(ReservationEntity reservation) {
    return reservation.getStatus() == ReservationStatus.PENDING
        || reservation.getStatus() == ReservationStatus.CONFIRMED
        || reservation.getStatus() == ReservationStatus.MAINTENANCE;
  }

  private void sendReservationConfirmedEmail(ReservationEntity reservation) {
    ClientEntity client = clientForNotification(reservation);
    if (client == null) {
      return;
    }

    emailService.sendReservationConfirmedEmail(
        client.getEmail(),
        client.getName(),
        sportLabel(reservation.getSport()),
        reservation.getCourt().getName(),
        reservation.getReservationDate(),
        reservation.getReservationTime()
    );
  }

  private void sendReservationCancelledEmail(ReservationEntity reservation) {
    ClientEntity client = clientForNotification(reservation);
    if (client == null) {
      return;
    }

    emailService.sendReservationCancelledEmail(
        client.getEmail(),
        client.getName(),
        sportLabel(reservation.getSport()),
        reservation.getCourt().getName(),
        reservation.getReservationDate(),
        reservation.getReservationTime()
    );
  }

  private void sendMaintenanceCancellationEmail(ReservationEntity reservation, boolean refunded) {
    ClientEntity client = clientForNotification(reservation);
    if (client == null) {
      return;
    }

    emailService.sendMaintenanceCancellationEmail(
        client.getEmail(),
        client.getName(),
        sportLabel(reservation.getSport()),
        reservation.getCourt().getName(),
        reservation.getReservationDate(),
        reservation.getReservationTime(),
        refunded
    );
  }

  private ClientEntity clientForNotification(ReservationEntity reservation) {
    if (reservation.getClientId() == null || reservation.getStatus() == ReservationStatus.MAINTENANCE) {
      return null;
    }

    try {
      return clientService.getEntityById(reservation.getClientId());
    } catch (RuntimeException ex) {
      log.warn("Reservation email skipped because client {} was not found", reservation.getClientId());
      return null;
    }
  }

  private String sportLabel(SportType sport) {
    return switch (sport) {
      case FUTBOL -> "Fútbol";
      case PADEL -> "Pádel";
    };
  }
}

