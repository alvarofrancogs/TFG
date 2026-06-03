package com.alvar.oasisclub.events.service;

import com.alvar.oasisclub.clients.entity.ClientEntity;
import com.alvar.oasisclub.clients.service.ClientService;
import com.alvar.oasisclub.common.email.EmailService;
import com.alvar.oasisclub.events.dto.CreateEventRequest;
import com.alvar.oasisclub.events.dto.EventRegistrationResponse;
import com.alvar.oasisclub.events.dto.EventResponse;
import com.alvar.oasisclub.events.entity.EventEntity;
import com.alvar.oasisclub.events.entity.EventRegistrationEntity;
import com.alvar.oasisclub.events.repository.EventRegistrationRepository;
import com.alvar.oasisclub.events.repository.EventRepository;
import com.alvar.oasisclub.courts.repository.CourtRepository;
import com.alvar.oasisclub.reservations.dto.CreateMaintenanceBlockRequest;
import com.alvar.oasisclub.reservations.entity.ReservationStatus;
import com.alvar.oasisclub.reservations.repository.ReservationRepository;
import com.alvar.oasisclub.reservations.service.ReservationService;
import com.alvar.oasisclub.schedule.service.ScheduleSlotService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

  private static final Logger log = LoggerFactory.getLogger(EventService.class);

  private final EventRepository eventRepository;
  private final EventRegistrationRepository registrationRepository;
  private final ClientService clientService;
  private final EmailService emailService;
  private final ScheduleSlotService scheduleSlotService;
  private final ReservationService reservationService;
  private final CourtRepository courtRepository;
  private final ReservationRepository reservationRepository;

  public EventService(
      EventRepository eventRepository,
      EventRegistrationRepository registrationRepository,
      ClientService clientService,
      EmailService emailService,
      ScheduleSlotService scheduleSlotService,
      @Lazy ReservationService reservationService,
      CourtRepository courtRepository,
      ReservationRepository reservationRepository
  ) {
    this.eventRepository = eventRepository;
    this.registrationRepository = registrationRepository;
    this.clientService = clientService;
    this.emailService = emailService;
    this.scheduleSlotService = scheduleSlotService;
    this.reservationService = reservationService;
    this.courtRepository = courtRepository;
    this.reservationRepository = reservationRepository;
  }

  @Transactional
  public EventResponse createEvent(CreateEventRequest request) {
    if (!request.getStartTime().isBefore(request.getEndTime())) {
      throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
    }

    EventEntity event = EventEntity.builder()
        .title(request.getTitle())
        .description(request.getDescription())
        .eventDate(request.getEventDate())
        .startTime(request.getStartTime())
        .endTime(request.getEndTime())
        .maxCapacity(request.getMaxCapacity())
        .category(request.getCategory())
        .sport(request.getSport())
        .isActive(true)
        .createdAt(LocalDateTime.now())
        .build();

    EventEntity saved = eventRepository.save(event);

    if (request.getSport() != null
        && request.getCourtIds() != null
        && !request.getCourtIds().isEmpty()) {
      String names = request.getCourtIds().stream()
          .map(id -> courtRepository.findById(id)
              .map(c -> c.getName())
              .orElse("Pista"))
          .collect(java.util.stream.Collectors.joining(", "));
      saved.setCourtNames(names);
      eventRepository.save(saved);
      blockCourtsForEvent(request, saved);
    }

    return toResponse(saved, 0L, false);
  }

  private void blockCourtsForEvent(CreateEventRequest request, EventEntity event) {
    List<String> allSlots = scheduleSlotService.getAllSlots();
    List<String> slotsInRange = allSlots.stream()
        .filter(slot -> {
          LocalTime slotTime = LocalTime.parse(slot);
          return !slotTime.isBefore(request.getStartTime())
              && slotTime.isBefore(request.getEndTime());
        })
        .toList();

    for (UUID courtId : request.getCourtIds()) {
      for (String slot : slotsInRange) {
        try {
          CreateMaintenanceBlockRequest blockReq = new CreateMaintenanceBlockRequest();
          blockReq.setSport(request.getSport());
          blockReq.setCourtId(courtId);
          blockReq.setDate(request.getEventDate());
          blockReq.setTime(LocalTime.parse(slot));
          reservationService.createMaintenanceBlock(blockReq);
        } catch (Exception ex) {
          log.warn("Could not block court {} at slot {} for event {}: {}",
              courtId, slot, event.getId(), ex.getMessage());
        }
      }
    }
  }

  @Transactional(readOnly = true)
  public List<EventResponse> getActiveEvents(UUID clientId) {
    List<EventEntity> events = eventRepository
        .findByIsActiveTrueAndEventDateGreaterThanEqualOrderByEventDateAscStartTimeAsc(LocalDate.now());
    return events.stream()
        .map(e -> toResponse(e,
            registrationRepository.countByEventId(e.getId()),
            clientId != null && registrationRepository.existsByEventIdAndClientId(e.getId(), clientId)))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<EventResponse> getAllEvents() {
    return eventRepository.findAllByOrderByEventDateAscStartTimeAsc().stream()
        .map(e -> toResponse(e, registrationRepository.countByEventId(e.getId()), false))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<EventRegistrationResponse> getRegistrations(UUID eventId) {
    EventEntity event = requireEvent(eventId);
    List<EventRegistrationEntity> regs = registrationRepository.findByEventIdOrderByCreatedAtAsc(eventId);
    List<EventRegistrationResponse> result = new ArrayList<>();
    for (EventRegistrationEntity reg : regs) {
      try {
        ClientEntity client = clientService.getEntityById(reg.getClientId());
        result.add(new EventRegistrationResponse(
            reg.getId(),
            client.getId(),
            client.getName(),
            client.getEmail(),
            reg.getCreatedAt()
        ));
      } catch (Exception ex) {
        log.warn("Client {} not found for registration {} in event {}",
            reg.getClientId(), reg.getId(), event.getId());
      }
    }
    return result;
  }

  @Transactional
  public void register(UUID eventId, UUID clientId) {
    EventEntity event = requireEvent(eventId);

    if (!event.getIsActive()) {
      throw new IllegalArgumentException("El evento no está activo");
    }
    if (event.getEventDate().isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("El evento ya ha finalizado");
    }
    if (registrationRepository.existsByEventIdAndClientId(eventId, clientId)) {
      throw new IllegalArgumentException("Ya estás inscrito en este evento");
    }

    long count = registrationRepository.countByEventId(eventId);
    if (count >= event.getMaxCapacity()) {
      throw new IllegalArgumentException("El evento está completo");
    }

    EventRegistrationEntity reg = EventRegistrationEntity.builder()
        .event(event)
        .clientId(clientId)
        .createdAt(LocalDateTime.now())
        .build();
    registrationRepository.save(reg);

    try {
      ClientEntity client = clientService.getEntityById(clientId);
      emailService.sendEventRegistrationEmail(
          client.getEmail(),
          client.getName(),
          event.getTitle(),
          event.getEventDate(),
          event.getStartTime(),
          event.getEndTime()
      );
    } catch (Exception ex) {
      log.warn("Could not send registration email for event {} client {}: {}",
          eventId, clientId, ex.getMessage());
    }
  }

  @Transactional
  public void unregister(UUID eventId, UUID clientId) {
    EventEntity event = requireEvent(eventId);
    if (!registrationRepository.existsByEventIdAndClientId(eventId, clientId)) {
      throw new IllegalArgumentException("No estás inscrito en este evento");
    }
    registrationRepository.deleteByEventIdAndClientId(eventId, clientId);

    try {
      ClientEntity client = clientService.getEntityById(clientId);
      emailService.sendEventUnregistrationEmail(
          client.getEmail(),
          client.getName(),
          event.getTitle(),
          event.getEventDate(),
          event.getStartTime(),
          event.getEndTime()
      );
    } catch (Exception ex) {
      log.warn("Could not send unregistration email for event {} client {}: {}",
          eventId, clientId, ex.getMessage());
    }
  }

  @Transactional
  public void adminRemoveRegistration(UUID eventId, UUID registrationId) {
    requireEvent(eventId);
    EventRegistrationEntity reg = registrationRepository.findById(registrationId)
        .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
    if (!reg.getEvent().getId().equals(eventId)) {
      throw new IllegalArgumentException("La inscripción no pertenece a este evento");
    }
    registrationRepository.delete(reg);
  }

  @Transactional
  public void deleteEvent(UUID eventId) {
    EventEntity event = requireEvent(eventId);
    eventRepository.delete(event);
    log.info("Event {} deleted", eventId);
  }

  @Transactional
  public void cleanupPastEvents() {
    List<EventEntity> past = eventRepository.findByEventDateBefore(LocalDate.now());
    if (!past.isEmpty()) {
      eventRepository.deleteAll(past);
      log.info("Auto-cleanup: {} past event(s) deleted", past.size());
    }
  }

  private EventEntity requireEvent(UUID eventId) {
    return eventRepository.findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado: " + eventId));
  }

  private EventResponse toResponse(EventEntity e, long registeredCount, boolean isRegistered) {
    String courtNames = resolveCourtsNames(e);
    return new EventResponse(
        e.getId(),
        e.getTitle(),
        e.getDescription(),
        e.getEventDate(),
        e.getStartTime(),
        e.getEndTime(),
        e.getMaxCapacity(),
        registeredCount,
        isRegistered,
        e.getCategory(),
        e.getSport(),
        courtNames,
        e.getIsActive(),
        e.getCreatedAt()
    );
  }

  /** Derives court names from MAINTENANCE reservations on the event date/time range. */
  private String resolveCourtsNames(EventEntity e) {
    if (e.getSport() == null) return null;
    if (e.getCourtNames() != null && !e.getCourtNames().isBlank()) return e.getCourtNames();
    try {
      List<String> names = reservationRepository
          .findByStatusAndReservationDateAndReservationTimeBetween(
              ReservationStatus.MAINTENANCE,
              e.getEventDate(),
              e.getStartTime(),
              e.getEndTime().minusMinutes(1)
          )
          .stream()
          .filter(r -> r.getCourt() != null)
          .map(r -> r.getCourt().getName())
          .distinct()
          .sorted()
          .collect(java.util.stream.Collectors.toList());
      return names.isEmpty() ? null : String.join(", ", names);
    } catch (Exception ex) {
      log.warn("Could not resolve court names for event {}: {}", e.getId(), ex.getMessage());
      return null;
    }
  }
}
