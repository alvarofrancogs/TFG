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
}