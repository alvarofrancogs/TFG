package com.alvar.oasisclub.schedule.service;

import com.alvar.oasisclub.reservations.entity.ReservationEntity;
import com.alvar.oasisclub.reservations.entity.ReservationStatus;
import com.alvar.oasisclub.reservations.repository.ReservationRepository;
import com.alvar.oasisclub.reservations.service.ReservationService;
import com.alvar.oasisclub.schedule.entity.ScheduleSlotEntity;
import com.alvar.oasisclub.schedule.repository.ScheduleSlotRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleSlotService {

  private static final Logger log = LoggerFactory.getLogger(ScheduleSlotService.class);

  private final ScheduleSlotRepository scheduleSlotRepository;
  private final ReservationRepository reservationRepository;
  private final ReservationService reservationService;

  public ScheduleSlotService(
      ScheduleSlotRepository scheduleSlotRepository,
      ReservationRepository reservationRepository,
      @Lazy ReservationService reservationService
  ) {
    this.scheduleSlotRepository = scheduleSlotRepository;
    this.reservationRepository = reservationRepository;
    this.reservationService = reservationService;
  }

  @Transactional(readOnly = true)
  public List<String> getAllSlots() {
    return scheduleSlotRepository.findAllByOrderBySlotTimeAsc().stream()
        .map(slot -> slot.getSlotTime().toString().substring(0, 5))
        .toList();
  }

  @Transactional
  public String addSlot(String time) {
    LocalTime parsed = LocalTime.parse(time);
    if (scheduleSlotRepository.existsBySlotTime(parsed)) {
      throw new IllegalArgumentException("El horario " + time + " ya existe");
    }
    ScheduleSlotEntity saved = scheduleSlotRepository.save(new ScheduleSlotEntity(parsed));
    return saved.getSlotTime().toString().substring(0, 5);
  }

  @Transactional
  public void removeSlot(String time) {
    LocalTime parsed = LocalTime.parse(time);
    if (!scheduleSlotRepository.existsBySlotTime(parsed)) {
      throw new IllegalArgumentException("El horario " + time + " no existe");
    }

    List<ReservationEntity> futureReservations = reservationRepository
        .findByReservationTimeAndReservationDateGreaterThanEqualAndStatusIn(
            parsed,
            LocalDate.now(),
            List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.MAINTENANCE)
        );

    for (ReservationEntity reservation : futureReservations) {
      if (reservation.getStatus() == ReservationStatus.MAINTENANCE) {
        reservationRepository.delete(reservation);
        log.info("Maintenance block {} deleted due to schedule slot removal", reservation.getId());
      } else {
        reservationService.cancelAndRefund(reservation);
        log.info("Reservation {} cancelled and refunded due to schedule slot removal", reservation.getId());
      }
    }

    scheduleSlotRepository.deleteBySlotTime(parsed);
  }
}
